/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 1999-2006
 *	Sleepycat Software.  All rights reserved.
 *
 * $Id: MemoryException.java,v 12.2 2006/01/02 22:02:36 bostic Exp $
 */
package com.sleepycat.db;

import com.sleepycat.db.internal.DbEnv;

public class MemoryException extends DatabaseException {
    private DatabaseEntry dbt = null;
    private String message;

    protected MemoryException(final String s,
                              final DatabaseEntry dbt,
                              final int errno,
                              final DbEnv dbenv) {
        super(s, errno, dbenv);
        this.message = s;
        this.dbt = dbt;
    }

    public DatabaseEntry getDatabaseEntry() {
        return dbt;
    }

    public String toString() {
        return message;
    }

    void updateDatabaseEntry(final DatabaseEntry newEntry) {
        if (this.dbt == null) {
            this.message = "DatabaseEntry not large enough for available data";
            this.dbt = newEntry;
        }
    }
}
