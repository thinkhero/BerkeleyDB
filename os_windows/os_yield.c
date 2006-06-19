/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 1997-2006
 *	Sleepycat Software.  All rights reserved.
 *
 * $Id: os_yield.c,v 12.6 2006/06/08 13:34:03 bostic Exp $
 */

#include "db_config.h"

#include "db_int.h"

/*
 * __os_yield --
 *	Yield the processor.
 */
void
__os_yield(dbenv)
	DB_ENV *dbenv;
{
	/*
	 * The call to Sleep(0) is specified by MSDN to yield the current
	 * thread's time slice to another thread of equal or greater priority.
	 */
	Sleep(0);
}
