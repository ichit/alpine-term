TERMUX_PKG_HOMEPAGE=http://www.dest-unreach.org/socat/
TERMUX_PKG_DESCRIPTION="Relay for bidirectional data transfer between two independent data channels"
TERMUX_PKG_LICENSE="GPL-2.0"
TERMUX_PKG_VERSION=1.7.3.2
TERMUX_PKG_SRCURL=http://www.dest-unreach.org/socat/download/socat-${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=ce3efc17e3e544876ebce7cd6c85b3c279fda057b2857fcaaf67b9ab8bdaf034
TERMUX_PKG_DEPENDS="readline"
TERMUX_PKG_BUILD_IN_SRC=yes

TERMUX_PKG_EXTRA_CONFIGURE_ARGS="
--disable-exec
--disable-openssl
--disable-system
ac_header_resolv_h=no
ac_cv_c_compiler_gnu=yes
ac_compiler_gnu=yes
"
