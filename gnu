
#------------------------------------------------------------------------------
# $File: gnu,v 1.11 2009/09/19 16:28:09 christos Exp $
# gnu:  file(1) magic for various GNU tools
#
# GNU nlsutils message catalog file format
#
0	string		\336\22\4\225	GNU message catalog (little endian),
>4	lelong		x		revision %d,
>8	lelong		x		%d messages
0	string		\225\4\22\336	GNU message catalog (big endian),
>4	belong		x		revision %d,
>8	belong		x		%d messages
# message catalogs, from Mitchum DSouza <m.dsouza@mrc-apu.cam.ac.uk>
0	string		*nazgul*	Nazgul style compiled message catalog
>8	belong		>0		\b, version %ld

# GnuPG
# The format is very similar to pgp
0	string          \001gpg                 GPG key trust database
>4	byte            x                       version %d
# Note: magic.mime had 0x8501 for the next line instead of 0x8502
0	beshort		0x8502			GPG encrypted data
!:mime	text/PGP # encoding: data

# This magic is not particularly good, as the keyrings don't have true
# magic. Nevertheless, it covers many keyrings.
0       beshort         0x9901                  GPG key public ring
!:mime	application/x-gnupg-keyring

# Gnumeric spreadsheet
# This entry is only semi-helpful, as Gnumeric compresses its files, so
# they will ordinarily reported as "compressed", but at least -z helps
39      string          =<gmr:Workbook           Gnumeric spreadsheet

# From: James Youngman <jay@gnu.org> 
# gnu find magic
0	string	\0LOCATE	GNU findutils locate database data
>7	string	>\0		\b, format %s
>7	string	02		\b (frcode)

# Files produced by GNU gettext
0	long	0xDE120495		GNU-format message catalog data
0	long	0x950412DE		GNU-format message catalog data
