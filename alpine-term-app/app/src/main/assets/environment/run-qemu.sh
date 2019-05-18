#!/usr/bin/env bash
##
##  Debugging script to run OS images supplied with Alpine Term.
##  Make sure that run-socat.sh is started before this script.
##

set -e -u

# Use KVM to make things faster.
set -- "-enable-kvm"

# Set ram amount to 4GB.
set -- "${@}" "-m" "4096M"

# Do not create default devices.
set -- "${@}" "-nodefaults"

# Setup primary CD-ROM (with OS live image).
set -- "${@}" "-drive" "file=${PWD}/os_cdrom.iso,index=0,media=cdrom,if=ide"

# Setup primary hard drive image (with the main OS installation).
set -- "${@}" "-device" "virtio-scsi-pci"
set -- "${@}" "-drive" "file=${PWD}/os_image.qcow2,if=none,discard=unmap,cache=writeback,id=hd0"
set -- "${@}" "-device" "scsi-hd,drive=hd0"

# Allow to select boot device.
set -- "${@}" "-boot" "c,menu=on"

# Comment this to disable snapshot mode.
set -- "${@}" "-snapshot"

# Use virtio RNG. Provides a faster RNG for the guest OS.
set -- "${@}" "-object" "rng-random,filename=/dev/urandom,id=rng0"
set -- "${@}" "-device" "virtio-rng-pci,rng=rng0"

# Setup networking.
set -- "${@}" "-netdev" "user,id=vmnic0"
set -- "${@}" "-device" "virtio-net,netdev=vmnic0"

# Disable graphical output.
set -- "${@}" "-vga" "none"
set -- "${@}" "-nographic"

# Monitor.
set -- "${@}" "-chardev" "tty,id=monitor0,mux=off,path=$(tty)"
set -- "${@}" "-monitor" "chardev:monitor0"

# Setup serial console output.
set -- "${@}" "-chardev" "socket,id=console0,path=/tmp/qemu-serial.sock"
set -- "${@}" "-serial" "chardev:console0"

# Disable parallel port.
set -- "${@}" "-parallel" "none"

qemu-system-x86_64 "$@" || true
stty sane
