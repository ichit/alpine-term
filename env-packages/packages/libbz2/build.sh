TERMUX_PKG_HOMEPAGE=http://www.bzip.org/
TERMUX_PKG_DESCRIPTION="BZ2 format compression library"
TERMUX_PKG_LICENSE="BSD"
TERMUX_PKG_VERSION=1.0.8
TERMUX_PKG_SRCURL=https://fossies.org/linux/misc/bzip2-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=47fd74b2ff83effad0ddf62074e6fad1f6b4a77a96e121ab421c20a216371a1f
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
