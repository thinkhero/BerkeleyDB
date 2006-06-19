/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2000-2006
 *      Sleepycat Software.  All rights reserved.
 *
 * $Id: TransactionRunner.java,v 12.4 2006/01/02 22:02:32 bostic Exp $
 */

package com.sleepycat.collections;

import com.sleepycat.compat.DbCompat;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DeadlockException;
import com.sleepycat.db.Environment;
import com.sleepycat.db.Transaction;
import com.sleepycat.db.TransactionConfig;
import com.sleepycat.util.ExceptionUnwrapper;

/**
 * Starts a transaction, calls {@link TransactionWorker#doWork}, and handles
 * transaction retry and exceptions.  To perform a transaction, the user
 * implements the {@link TransactionWorker} interface and passes an instance of
 * that class to the {@link #run} method.
 *
 * <p>A single TransactionRunner instance may be used by any number of threads
 * for any number of transactions.</p>
 *
 * <p>The behavior of the run() method depends on whether the environment is
 * transactional, whether nested transactions are enabled, and whether a
 * transaction is already active.</p>
 *
 * <ul>
 * <li>When the run() method is called in a transactional environment and no
 * transaction is active for the current thread, a new transaction is started
 * before calling doWork().  If DeadlockException is thrown by doWork(), the
 * transaction will be aborted and the process will be repeated up to the
 * maximum number of retries.  If another exception is thrown by doWork() or
 * the maximum number of retries has occurred, the transaction will be aborted
 * and the exception will be rethrown by the run() method.  If no exception is
 * thrown by doWork(), the transaction will be committed.  The run() method
 * will not attempt to commit or abort a transaction if it has already been
 * committed or aborted by doWork().</li>
 *
 * <li>When the run() method is called and a transaction is active for the
 * current thread, and nested transactions are enabled, a nested transaction is
 * started before calling doWork().  The transaction that is active when
 * calling the run() method will become the parent of the nested transaction.
 * The nested transaction will be committed or aborted by the run() method
 * following the same rules described above.  Note that nested transactions may
 * not be enabled for the JE product, since JE does not support nested
 * transactions.</li>
 *
 * <li>When the run() method is called in a non-transactional environment, the
 * doWork() method is called without starting a transaction.  The run() method
 * will return without committing or aborting a transaction, and any exceptions
 * thrown by the doWork() method will be thrown by the run() method.</li>
 *
 * <li>When the run() method is called and a transaction is active for the
 * current thread and nested transactions are not enabled (the default) the
 * same rules as above apply. All the operations performed by the doWork()
 * method will be part of the currently active transaction.</li>
 * </ul>
 *
 * <p>In a transactional environment, the rules described above support nested
 * calls to the run() method and guarantee that the outermost call will cause
 * the transaction to be committed or aborted.  This is true whether or not
 * nested transactions are supported or enabled.  Note that nested transactions
 * are provided as an optimization for improving concurrency but do not change
 * the meaning of the outermost transaction.  Nested transactions are not
 * currently supported by the JE product.</p>
 *
 * @author Mark Hayes
 */
public class TransactionRunner {

    /** The default maximum number of retries. */
    public static final int DEFAULT_MAX_RETRIES = 10;

    private Environment env;
    private CurrentTransaction currentTxn;
    private int maxRetries;
    private TransactionConfig config;
    private boolean allowNestedTxn;

    /**
     * Creates a transaction runner for a given Berkeley DB environment.
     * The default maximum number of retries ({@link #DEFAULT_MAX_RETRIES}) and
     * a null (default) {@link TransactionConfig} will be used.
     *
     * @param env is the environment for running transactions.
     */
    public TransactionRunner(Environment env) {

        this(env, DEFAULT_MAX_RETRIES, null);
    }

    /**
     * Creates a transaction runner for a given Berkeley DB environment and
     * with a given number of maximum retries.
     *
     * @param env is the environment for running transactions.
     *
     * @param maxRetries is the maximum number of retries that will be
     * performed when deadlocks are detected.
     *
     * @param config the transaction configuration used for calling
     * {@link Environment#beginTransaction}, or null to use the default
     * configuration.  The configuration object is not cloned, and
     * any modifications to it will impact subsequent transactions.
     */
    public TransactionRunner(Environment env, int maxRetries,
                             TransactionConfig config) {

        this.env = env;
        this.currentTxn = CurrentTransaction.getInstance(env);
        this.maxRetries = maxRetries;
        this.config = config;
    }

    /**
     * Returns the maximum number of retries that will be performed when
     * deadlocks are detected.
     */
    public int getMaxRetries() {

        return maxRetries;
    }

    /**
     * Changes the maximum number of retries that will be performed when
     * deadlocks are detected.
     * Calling this method does not impact transactions already running.
     */
    public void setMaxRetries(int maxRetries) {

        this.maxRetries = maxRetries;
    }

    /**
     * Returns whether nested transactions will be created if
     * <code>run()</code> is called when a transaction is already active for
     * the current thread.
     * By default this property is false.
     *
     * <p>Note that this method always returns false in the JE product, since
     * nested transactions are not supported by JE.</p>
     */
    public boolean getAllowNestedTransactions() {

        return allowNestedTxn;
    }

    /**
     * Changes whether nested transactions will be created if
     * <code>run()</code> is called when a transaction is already active for
     * the current thread.
     * Calling this method does not impact transactions already running.
     *
     * <p>Note that true may not be passed to this method in the JE product,
     * since nested transactions are not supported by JE.</p>
     */
    public void setAllowNestedTransactions(boolean allowNestedTxn) {

        if (allowNestedTxn && !DbCompat.NESTED_TRANSACTIONS) {
            throw new UnsupportedOperationException(
                    "Nested transactions are not supported.");
        }
        this.allowNestedTxn = allowNestedTxn;
    }

    /**
     * Returns the transaction configuration used for calling
     * {@link Environment#beginTransaction}.
     * 
     * <p>If this property is null, the default configuration is used.  The
     * configuration object is not cloned, and any modifications to it will
     * impact subsequent transactions.</p>
     *
     * @return the transaction configuration.
     */
    public TransactionConfig getTransactionConfig() {

        return config;
    }

    /**
     * Changes the transaction configuration used for calling
     * {@link Environment#beginTransaction}.
     * 
     * <p>If this property is null, the default configuration is used.  The
     * configuration object is not cloned, and any modifications to it will
     * impact subsequent transactions.</p>
     *
     * @param config the transaction configuration.
     */
    public void setTransactionConfig(TransactionConfig config) {

        this.config = config;
    }

    /**
     * Calls the {@link TransactionWorker#doWork} method and, for transactional
     * environments, may begin and end a transaction.  If the environment given
     * is non-transactional, a transaction will not be used but the doWork()
     * method will still be called.  See the class description for more
     * information.
     *
     * @throws DeadlockException when it is thrown by doWork() and the
     * maximum number of retries has occurred.  The transaction will have been
     * aborted by this method.
     *
     * @throws Exception when any other exception is thrown by doWork().  The
     * exception will first be unwrapped by calling {@link
     * ExceptionUnwrapper#unwrap}.  The transaction will have been aborted by
     * this method.
     */
    public void run(TransactionWorker worker)
        throws DatabaseException, Exception {

        if (currentTxn != null &&
            (allowNestedTxn || currentTxn.getTransaction() == null)) {

            /*
             * Transactional and (not nested or nested txns allowed).
             */
            for (int i = 0;; i += 1) {
                Transaction txn = null;
                try {
                    txn = currentTxn.beginTransaction(config);
                    worker.doWork();
                    if (txn != null && txn == currentTxn.getTransaction()) {
                        currentTxn.commitTransaction();
                    }
                    return;
                } catch (Throwable e) {
                    e = ExceptionUnwrapper.unwrapAny(e);
                    if (txn != null && txn == currentTxn.getTransaction()) {
                        try {
                            currentTxn.abortTransaction();
                        } catch (Throwable e2) {

                            /*
                             * XXX We should really throw a 3rd exception that
                             * wraps both e and e2, to give the user a complete
                             * set of error information.
                             */
                            e2.printStackTrace();
                            /* Force the original exception to be thrown. */
                            i = maxRetries + 1;
                        }
                    }
                    if (i >= maxRetries || !(e instanceof DeadlockException)) {
                        if (e instanceof Exception) {
                            throw (Exception) e;
                        } else {
                            throw (Error) e;
                        }
                    }
                }
            }
        } else {

            /*
             * Non-transactional or (nested and no nested txns allowed).
             */
            try {
                worker.doWork();
            } catch (Exception e) {
                throw ExceptionUnwrapper.unwrap(e);
            }
        }
    }
}
