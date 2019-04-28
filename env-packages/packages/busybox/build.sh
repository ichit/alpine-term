TERMUX_PKG_HOMEPAGE=https://busybox.net/
TERMUX_PKG_DESCRIPTION="Tiny versions of many common UNIX utilities into a single small executable"
TERMUX_PKG_VERSION=1.30.1
TERMUX_PKG_SRCURL=https://busybox.net/downloads/busybox-${TERMUX_PKG_VERSION}.tar.bz2
TERMUX_PKG_SHA256=3d1d04a4dbd34048f4794815a5c48ebb9eb53c5277e09ffffc060323b95dfbdc
TERMUX_PKG_BUILD_IN_SRC=yes

termux_step_configure () {
	cp ${TERMUX_PKG_BUILDER_DIR}/busybox.config .config
	echo "CONFIG_SYSROOT=\"$TERMUX_STANDALONE_TOOLCHAIN/sysroot\"" >> .config
	echo "CONFIG_PREFIX=\"$TERMUX_PREFIX\"" >> .config
	echo "CONFIG_CROSS_COMPILER_PREFIX=\"${TERMUX_HOST_PLATFORM}-\"" >> .config
	echo "CONFIG_FEATURE_CROND_DIR=\"$TERMUX_PREFIX/var/spool/cron\"" >> .config
	echo "CONFIG_SV_DEFAULT_SERVICE_DIR=\"$TERMUX_PREFIX/var/service\"" >> .config
	make oldconfig
}

termux_step_post_make_install () {
	if [ "$TERMUX_DEBUG" == "true" ]; then
		install -Dm755 busybox_unstripped "${TERMUX_PREFIX}/bin/busybox"
	else
		install -Dm755 busybox "${TERMUX_PREFIX}/bin/busybox"
	fi
}
