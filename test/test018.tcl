# See the file LICENSE for redistribution information.
#
# Copyright (c) 1996-2006
#	Sleepycat Software.  All rights reserved.
#
# $Id: test018.tcl,v 12.2 2006/01/02 22:03:25 bostic Exp $
#
# TEST	test018
# TEST	Offpage duplicate test
# TEST	Key_{first,last,before,after} offpage duplicates.
# TEST	Run duplicates with small page size so that we test off page
# TEST	duplicates.
proc test018 { method {nentries 10000} args} {
	puts "Test018: Off page duplicate tests"
	set pgindex [lsearch -exact $args "-pagesize"]
	if { $pgindex != -1 } {
		puts "Test018: Skipping for specific pagesizes"
		return
	}
	eval {test011 $method $nentries 19 "018" -pagesize 512} $args
}
