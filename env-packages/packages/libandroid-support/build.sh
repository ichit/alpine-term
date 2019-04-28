TERMUX_PKG_HOMEPAGE=https://github.com/termux/libandroid-support
TERMUX_PKG_DESCRIPTION="Library extending the Android C library (Bionic) for additional multibyte, locale and math support"
TERMUX_PKG_LICENSE="Apache-2.0"
TERMUX_PKG_VERSION=24
TERMUX_PKG_SRCURL=https://github.com/termux/libandroid-support/archive/v${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=e14e262429a60bea733d5bed69d2f3a1cada53fcadaf76787fca5c8b0d4dae2f
TERMUX_PKG_BUILD_IN_SRC=yes

termux_step_make_install() {
	_C_FILES="src/musl-*/*.c"
	$CC $CFLAGS -std=c99 -DNULL=0 $CPPFLAGS $LDFLAGS \
		-Iinclude \
		$_C_FILES \
		-fpic \
		-c

	$AR rcs libandroid-support.a *.o

	cp libandroid-support.a $TERMUX_PREFIX/lib/
	(cd $TERMUX_PREFIX/lib; ln -f -s libandroid-support.a libiconv.a; )
}
