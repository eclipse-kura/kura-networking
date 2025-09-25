#!/bin/bash
#
#  Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
#
#  This program and the accompanying materials are made
#  available under the terms of the Eclipse Public License 2.0
#  which is available at https://www.eclipse.org/legal/epl-2.0/
#
#  SPDX-License-Identifier: EPL-2.0
#
#  Contributors:
#   Eurotech
#

setup_libudev() {
    # create soft link for libudev.so.0 to make it retrocompatible
    # https://unix.stackexchange.com/questions/156776/arch-ubuntu-so-whats-the-deal-with-libudev-so-0
    if [ ! -f /lib/libudev.so.0 ] && [ -f /lib/libudev.so.1 ]; then
        ln -sf /lib/libudev.so.1 /lib/libudev.so.0
    fi

    if uname -m | grep -q arm ; then
        destination="/usr/lib/arm-linux-gnueabihf/libudev.so.1"
        link_name="/usr/lib/arm-linux-gnueabihf/libudev.so.0"
    fi
    if uname -m | grep -q aarch ; then
        destination="/usr/lib/aarch64-linux-gnu/libudev.so.1"
        link_name="/usr/lib/aarch64-linux-gnu/libudev.so.0"
    fi
    if uname -m | grep -q x86_64 ; then
         destination="/usr/lib/x86_64-linux-gnu/libudev.so.1"
        link_name="/usr/lib/x86_64-linux-gnu/libudev.so.0"
    fi

    if [ -f "${destination}" ] && [ ! -f "${link_name}" ]; then
        echo "Setting up symlink ${link_name} -> ${destination}"
        ln -sf "${destination}" "${link_name}"
    fi
}

customize_snapshot() {
    python3 "/opt/eclipse/kura/kura-networking-install/customize_snapshot_network.py" "/opt/eclipse/kura/user/snapshots/snapshot_0.xml"
}

customize_iptables() {
    if [ "${IS_NETWORKING_PROFILE}" = "true" ]; then
        mv "/opt/eclipse/kura/kura-networking-install/iptables" "/opt/eclipse/kura/.data/iptables"
        python3 "/opt/eclipse/kura/kura-networking-install/customize_iptables.py"
    fi
}

IS_NETWORKING_PROFILE=$1

setup_libudev

BOARD="generic-device"
if uname -a | grep -q 'raspberry' > /dev/null 2>&1
then
    BOARD="raspberry"
    echo "Customizing installation for Raspberry PI"
fi

customize_snapshot
customize_iptables
