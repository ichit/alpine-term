TERMUX_PKG_HOMEPAGE=http://www.bzip.org/
TERMUX_PKG_DESCRIPTION="BZ2 format compression library"
TERMUX_PKG_LICENSE="BSD"
TERMUX_PKG_VERSION=1.0.6
TERMUX_PKG_SRCURL=https://fossies.org/linux/misc/bzip2-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=4bbea71ae30a0e5a8ddcee8da750bc978a479ba11e04498d082fa65c2f8c1ad5
TERMUX_PKG_EXTRA_MAKE_ARGS="PREFIX=$TERMUX_PREFIX"
TERMUX_PKG_BUILD_IN_SRC=yes

termux_step_configure() {
	# bzip2 does not use configure. But place man pages at correct path:
	sed -i "s@(PREFIX)/man@(PREFIX)/share/man@g" $TERMUX_PKG_SRCDIR/Makefile
}

termux_step_make() {
	# bzip2 uses a separate makefile for the shared library
	make libbz2.a
}

termux_step_make_install() {
	install -Dm600 libbz2.a $TERMUX_PREFIX/lib/libbz2.a
	install -Dm600 bzlib.h $TERMUX_PREFIX/include/bzlib.h
}
