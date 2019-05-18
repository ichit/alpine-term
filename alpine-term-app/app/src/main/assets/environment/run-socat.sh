#!/usr/bin/env bash
##
##  Start socat listening on unix socket.
##  Make sure that run-qemu.sh is executed after this script.
##

socat "$(tty)",rawer "UNIX-LISTEN:/tmp/qemu-serial.sock,unlink-early"
