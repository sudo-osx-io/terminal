
#------------------------------------------------------------------------------
# $File: lua,v 1.5 2009/09/19 16:28:10 christos Exp $
# lua:  file(1) magic for Lua scripting language
# URL:  http://www.lua.org/
# From: Reuben Thomas <rrt@sc3d.org>, Seo Sanghyeon <tinuviel@sparcs.kaist.ac.kr>

# Lua scripts
0	search/1/w	#!\ /usr/bin/lua	Lua script text executable
!:mime	text/x-lua
0	search/1/w	#!\ /usr/local/bin/lua	Lua script text executable
!:mime	text/x-lua
0	search/1	#!/usr/bin/env\ lua	Lua script text executable
!:mime	text/x-lua
0	search/1	#!\ /usr/bin/env\ lua	Lua script text executable
!:mime	text/x-lua

# Lua bytecode
0	string		\033Lua			Lua bytecode,
>4	byte		0x50			version 5.0
>4	byte		0x51			version 5.1
