/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *	Sleepycat Software.  All rights reserved.
 *
 * $Id: EventType.java,v 1.1 2006/05/04 05:24:05 alexg Exp $
 */

package com.sleepycat.db;

import com.sleepycat.db.internal.DbConstants;

public final class EventType {

    public static final EventType PANIC =
        new EventType("PANIC", DbConstants.DB_EVENT_PANIC);

    public static final EventType REP_CLIENT =
        new EventType("REP_CLIENT", DbConstants.DB_EVENT_REP_CLIENT);

    public static final EventType REP_MASTER =
        new EventType("REP_MASTER", DbConstants.DB_EVENT_REP_MASTER);

    public static final EventType REP_NEW_MASTER =
        new EventType("REP_NEW_MASTER", DbConstants.DB_EVENT_REP_NEWMASTER);

    /* package */
    public static EventType fromInt(int type) {
        switch(type) {
        case DbConstants.DB_EVENT_PANIC:
            return PANIC;
        case DbConstants.DB_EVENT_REP_CLIENT:
            return REP_CLIENT;
        case DbConstants.DB_EVENT_REP_MASTER:
            return REP_MASTER;
        case DbConstants.DB_EVENT_REP_NEWMASTER:
            return REP_NEW_MASTER;
        default:
            throw new IllegalArgumentException(
                "Unknown event type: " + type);
        }
    }

    private String statusName;
    private int id;

    private EventType(final String statusName, final int id) {
        this.statusName = statusName;
        this.id = id;
    }

    /* package */
    int getId() {
        return id;
    }

    public String toString() {
        return "EventType." + statusName;
    }
}

