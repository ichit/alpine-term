TERMUX_PKG_HOMEPAGE=https://developer.gnome.org/glib/
TERMUX_PKG_DESCRIPTION="Library providing core building blocks for libraries and applications written in C"
TERMUX_PKG_LICENSE="LGPL-2.1"
TERMUX_PKG_VERSION=2.58.3
TERMUX_PKG_SRCURL=https://ftp.gnome.org/pub/gnome/sources/glib/${TERMUX_PKG_VERSION:0:4}/glib-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=8f43c31767e88a25da72b52a40f3301fefc49a665b56dc10ee7cc9565cbe7481
TERMUX_PKG_DEPENDS="libandroid-support, libffi, pcre, zlib"

# --enable-compile-warnings=no to get rid of format strings causing errors.
# --disable-znodelete to avoid DF_1_NODELETE which most Android 5.0 linkers does not support.
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="
--cache-file=termux_configure.cache
--disable-compile-warnings
--disable-gtk-doc
--disable-gtk-doc-html
--disable-libelf
--disable-libmount
--disable-znodelete
--with-pcre=system
"

termux_step_pre_configure () {
	NOCONFIGURE=1 ./autogen.sh

	# glib checks for __BIONIC__ instead of __ANDROID__:
	CFLAGS="$CFLAGS -D__BIONIC__=1"
	LDFLAGS="${LDFLAGS/-static/}"

	cd "${TERMUX_PKG_BUILDDIR}"

	# https://developer.gnome.org/glib/stable/glib-cross-compiling.html
	echo "glib_cv_long_long_format=ll" >> termux_configure.cache
	echo "glib_cv_stack_grows=no" >> termux_configure.cache
	echo "glib_cv_uscore=no" >> termux_configure.cache
	chmod a-w termux_configure.cache
}
