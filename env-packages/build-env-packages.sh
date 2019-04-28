#!/bin/bash -e
##
##	RUN THIS SCRIPT ONLY FROM DOCKER IMAGE !!!
##

# Only AArch64 is supported currently since ARM is
# too unstable, i686/x86_64 crash on QEMU reboot.
TARGET_ARCHITECTURES="aarch64"

ENV_BASE_DIR="$(dirname $(realpath "${0}"))/environment"
BUILD_PREFIX="/data/data/xeffyr.alpine.term/files/environment"

if [ ! -e "${ENV_BASE_DIR}" ]; then
	mkdir -p "${ENV_BASE_DIR}"
fi

for env_arch in ${TARGET_ARCHITECTURES}; do
	rm -rf "${ENV_BASE_DIR}/${env_arch}"
	rm -f "${ENV_BASE_DIR}/${env_arch}.zip"
	rm -f "${ENV_BASE_DIR}/${env_arch}.bin"
	mkdir -p "${ENV_BASE_DIR}/${env_arch}"

	for pkg in bash busybox openssl qemu-system-headless socat; do
		./build-package.sh -a "${env_arch}" "${pkg}"
	done

	echo "[*] Building environment package for '${env_arch}':"
#	echo "[*] = Installing programs..."

#	for bin in bash busybox openssl qemu-img socat tput; do
#		install -Dm700 "${BUILD_PREFIX}/bin/${bin}" "${ENV_BASE_DIR}/${env_arch}/bin/${bin}"
#	done
#	install -Dm700 "${BUILD_PREFIX}/bin/qemu-system-x86_64" "${ENV_BASE_DIR}/${env_arch}/bin/qemu"

#	echo "[*] = Stripping binaries..."
#	if [ "${env_arch}" = "aarch64" ]; then
#		find "${ENV_BASE_DIR}/${env_arch}"/bin/ -type f -exec ${HOME}/.alpine-term-build/_cache/19b-aarch64-24-v1/bin/aarch64-linux-android-strip -s "{}" \;
#	fi
#	${HOME}/.alpine-term-build/_cache/termux-elf-cleaner "${ENV_BASE_DIR}/${env_arch}"/bin/*

	echo "[*] = Installing data..."
	mkdir -p "${ENV_BASE_DIR}/${env_arch}/etc"
	install -Dm600 "${BUILD_PREFIX}/etc/inputrc" "${ENV_BASE_DIR}/${env_arch}/etc/inputrc"
	echo "nameserver 208.67.220.220" > "${ENV_BASE_DIR}/${env_arch}/etc/resolv.conf"
	mkdir -p "${ENV_BASE_DIR}/${env_arch}/share"
	cp -a "${BUILD_PREFIX}/share/tabset" "${ENV_BASE_DIR}/${env_arch}/share/tabset"
	install -Dm600 "${BUILD_PREFIX}/share/terminfo/x/xterm-256color" "${ENV_BASE_DIR}/${env_arch}/share/terminfo/x/xterm-256color"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/bios-256k.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/bios-256k.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/bios.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/bios.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-e1000e.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-e1000e.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-e1000.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-e1000.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-eepro100.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-eepro100.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-ne2k_pci.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-ne2k_pci.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-pcnet.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-pcnet.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-rtl8139.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-rtl8139.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-virtio.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-virtio.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/efi-vmxnet3.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/efi-vmxnet3.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/kvmvapic.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/kvmvapic.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/linuxboot.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/linuxboot.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/linuxboot_dma.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/linuxboot_dma.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/multiboot.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/multiboot.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/pxe-e1000.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/pxe-e1000.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/pxe-eepro100.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/pxe-eepro100.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/pxe-ne2k_pci.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/pxe-ne2k_pci.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/pxe-pcnet.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/pxe-pcnet.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/pxe-rtl8139.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/pxe-rtl8139.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/pxe-virtio.rom" "${ENV_BASE_DIR}/${env_arch}/share/qemu/pxe-virtio.rom"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/sgabios.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/sgabios.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/trace-events-all" "${ENV_BASE_DIR}/${env_arch}/share/qemu/trace-events-all"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/vgabios.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/vgabios.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/vgabios-cirrus.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/vgabios-cirrus.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/vgabios-qxl.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/vgabios-qxl.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/vgabios-stdvga.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/vgabios-stdvga.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/vgabios-virtio.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/vgabios-virtio.bin"
	install -Dm600 "${BUILD_PREFIX}/share/qemu/vgabios-vmware.bin" "${ENV_BASE_DIR}/${env_arch}/share/qemu/vgabios-vmware.bin"
	cp -a "${BUILD_PREFIX}/share/qemu/keymaps" "${ENV_BASE_DIR}/${env_arch}/share/qemu/keymaps"
	mkdir "${ENV_BASE_DIR}/${env_arch}/tmp"

#	cat <<- EOF > "${ENV_BASE_DIR}/${env_arch}/EXECUTABLES.txt"
#	bin/bash
#	bin/busybox
#	bin/openssl
#	bin/qemu
#	bin/qemu-img
#	bin/socat
#	bin/tput
#	EOF

	echo "[*] = Archiving..."
	(cd "${ENV_BASE_DIR}/${env_arch}"
		find . -type f | xargs chmod 600
		find . -type d | xargs chmod 700
		find . | xargs touch -c -h
#		zip -r9q "${env_arch}.zip" bin etc share tmp EXECUTABLES.txt
		zip -r9q "${env_arch}.zip" bin etc share tmp
		mv "${env_arch}.zip" "../${env_arch}.bin"
	)

	echo "[*] Done. Package located at: ${ENV_BASE_DIR}/${env_arch}.bin"
done
