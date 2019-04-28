TERMUX_PKG_HOMEPAGE=https://www.qemu.org
TERMUX_PKG_DESCRIPTION="A generic and open source machine emulator and virtualizer"
TERMUX_PKG_LICENSE="LGPL-2.1"
TERMUX_PKG_VERSION=4.0.0
TERMUX_PKG_SRCURL=https://download.qemu.org/qemu-${TERMUX_PKG_VERSION}.tar.xz
TERMUX_PKG_SHA256=13a93dfe75b86734326f8d5b475fde82ec692d5b5a338b4262aeeb6b0fa4e469
TERMUX_PKG_DEPENDS="attr, capstone, glib, libandroid-support, libbz2, libcap, liblzo, libpixman, ncurses, zlib"
TERMUX_PKG_BUILD_IN_SRC=true

termux_step_configure() {
	./configure \
		--prefix="${TERMUX_PREFIX}" \
		--cross-prefix="${TERMUX_HOST_PLATFORM}-" \
		--host-cc="gcc" \
		--cc="${CC}" \
		--cxx="${CXX}" \
		--objcc="${CC}" \
		--extra-ldflags="${LDFLAGS} -lm" \
		--enable-curses \
		--enable-vnc \
		--enable-coroutine-pool \
		--enable-virtfs \
		--enable-trace-backends=nop \
		--disable-hax \
		--disable-kvm \
		--disable-xen \
		--disable-fdt \
		--disable-guest-agent \
		--disable-stack-protector \
		--target-list=x86_64-softmmu
}
