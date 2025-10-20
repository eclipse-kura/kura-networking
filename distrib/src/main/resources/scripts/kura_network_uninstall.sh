#!/bin/bash
#
#  Copyright (c) 2020, 2025 Eurotech and/or its affiliates and others
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

STATUS=$1
BASE_DIR=$2
KURA_SYMLINK=$3

# shellcheck source=/dev/null
source "${BASE_DIR}/${KURA_SYMLINK}/.data/install_config.sh"

restore_backup_files() {
    SUFFIX="${1}"

    shift

    for file in "${@}"
    do
        if [ -f "${file}" ] && expr "${file}" : ".*[.]${SUFFIX}$" > /dev/null; then
            mv "${file}" "${file%."${SUFFIX}"}"
        fi
    done
}

restore_named_config() {
    mv /var/named/named.ca.kurasave /var/named/named.ca
    mv /etc/bind/named.conf.kurasave /etc/bind/named.conf
    mv /etc/named.rfc1912.zones.kurasave /etc/named.rfc1912.zones

    if [ -d /etc/apparmor.d ]; then
        rm /etc/apparmor.d/usr.sbin.named
    fi
}

restore_netplan() {
  if [ -f /etc/netplan/zz-kura-use-nm.yaml ]; then
    rm -f /etc/netplan/zz-kura-use-nm.yaml
  fi

  restore_backup_files kurasave /{lib,etc}/netplan/*
}

restore_nm_installation() {
    # restore /etc/network/interfaces.kurasave
    if test -f /etc/network/interfaces.kurasave; then
        mv /etc/network/interfaces.kurasave /etc/network/interfaces
    fi

    # restore iptables
    if test -f /etc/sysconfig/iptables.kurasave; then
        mv /etc/sysconfig/iptables.kurasave /etc/sysconfig/iptables
    fi
    if test -f /proc/sys/net/ipv4/ip_forward.kurasave; then
        mv "${BASE_DIR}/${KURA_SYMLINK}/.data/ip_forward.kurasave" /proc/sys/net/ipv4/ip_forward 
    fi
    
    # restore /etc/default/dnsmasq.kurasave
    if [ -f /etc/default/dnsmasq.kurasave ]; then
        mv /etc/default/dnsmasq.kurasave /etc/default/dnsmasq
    fi

    restore_named_config

    restore_netplan
}

recover_dnsmasq_conf_file() {
    DNSMASQ_CONF_FILE="/etc/dnsmasq.conf"
    if [ -f "${DNSMASQ_CONF_FILE}.kurasave" ]; then
        mv "${DNSMASQ_CONF_FILE}.kurasave" "${DNSMASQ_CONF_FILE}"
    fi

    DNSMASQ_LXC_FILE="/etc/dnsmasq.d/lxc"
    if [ -f "/etc/lxc.kurasave" ]; then
        mv "/etc/lxc.kurasave" "${DNSMASQ_LXC_FILE}"
    fi
}

remove_dnsmasq_leases() {
    # Remove dnsmasq leases file if it exists and is owned by 'kurad'
    DNSMASQ_LEASES_FILE="/var/lib/dhcp/dnsmasq.leases"
    if [ -f "${DNSMASQ_LEASES_FILE}" ]; then
        FILE_OWNER=$(stat -c '%U' "${DNSMASQ_LEASES_FILE}" 2>/dev/null)
        if [ "${FILE_OWNER}" = "kurad" ]; then
            rm -f "${DNSMASQ_LEASES_FILE}"
            echo "The dnsmasq leases file has been successfully removed."
        fi
    fi
}

recover_web_ui_kura_properties() {
    if [ -f "${BASE_DIR}/${KURA_SYMLINK}/framework/kura.properties" ]; then
        sed -i "s|^kura.have.net.admin=.*|kura.have.net.admin=false|" "${BASE_DIR}/${KURA_SYMLINK}/framework/kura.properties"
    fi
}

remove_kura_networking_service() {
    if [ -f /usr/lib/systemd/system/kura.service.d/kura-networking.conf ]; then
        rm /usr/lib/systemd/system/kura.service.d/kura-networking.conf
    fi
}


kura_uninstall() {
    echo "Uninstalling Kura networking..."
    
    if [ "${STATUS}" = "remove" ]; then
        echo "Configuring Kura networking..."

        bash "${BASE_DIR}/${KURA_SYMLINK}/.data/manage_network_permissions.sh" -u
        restore_nm_installation
        recover_dnsmasq_conf_file

        recover_web_ui_kura_properties
        remove_kura_networking_service
    fi

    # flush all cached filesystem to disk
    sync

    echo "Finished."
}

run_kura_networking_uninstall() {
    kura_uninstall &
    PID=$!
    START=$(date +%s)
}

run_kura_networking_uninstall

remove_dnsmasq_leases

exit 0
