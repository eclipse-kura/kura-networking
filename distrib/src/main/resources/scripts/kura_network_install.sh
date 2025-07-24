#!/bin/bash
#
# Copyright (c) 2020, 2025 Eurotech and/or its affiliates
#
#  All rights reserved.
#

STATUS=$1
BASE_DIR=$2
KURA_SYMLINK=$3

# shellcheck source=/dev/null
source "${BASE_DIR}/${KURA_SYMLINK}/.data/install_network_config.sh"

error_state() {
    echo "$1"
    touch /tmp/install_error
    exit 1
}

backup_files() {
    SUFFIX="${1}"

    shift

    for file in "${@}"
    do
        if [ -f "${file}" ]; then
            mv "${file}" "${file}.${SUFFIX}"
        fi
    done
}

install_named_config() {
    echo "Installing configuration for named."

    if [ ! -d "/var/named" ]; then
        mkdir /var/named
    fi

    install --backup --suffix=.kurasave --mode=644 --owner=bind "${BASE_DIR}/${KURA_SYMLINK}/.data/named.ca" /var/named/named.ca
    install --backup --suffix=.kurasave --mode=644 --owner=bind "${BASE_DIR}/${KURA_SYMLINK}/.data/named.rfc1912.zones" /etc/named.rfc1912.zones

    if [ -d "/etc/apparmor.d" ]; then
        install --mode=644 --owner=bind "${BASE_DIR}/${KURA_SYMLINK}/.data/usr.sbin.named" /etc/apparmor.d/usr.sbin.named
    fi
    
    if [ ! -f "/etc/bind/rndc.key" ] ; then
        rndc-confgen -r /dev/urandom -a
    fi
    chown bind:bind /etc/bind/rndc.key
    chmod 600 /etc/bind/rndc.key

    if [ -f "/etc/bind/named.conf" ] ; then
        cp /etc/bind/named.conf /etc/bind/named.conf.kurasave
        chmod a+r /etc/bind/named.conf
    fi
    
    chown -R bind /var/named

    echo "named installed."
}

disable_netplan() {
    # disable netplan configuration files
    backup_files kurasave /{lib,etc}/netplan/*.yaml

    if [ -d /etc/netplan  ]; then

    # use NM renderer
        cat > /etc/netplan/zz-kura-use-nm.yaml <<EOF
network:
  version: 2
  renderer: NetworkManager
EOF
    fi
}

setup_network_manager() {
    systemctl daemon-reload
    systemctl disable firewalld
    systemctl disable iptables
    systemctl disable ip6tables
    systemctl enable firewall
    systemctl enable NetworkManager
    systemctl enable ModemManager
    systemctl stop dnsmasq
    systemctl disable dnsmasq
    systemctl stop isc-dhcp-server
    systemctl disable isc-dhcp-server
    systemctl stop isc-dhcp-server6
    systemctl disable isc-dhcp-server6
    systemctl stop hostapd
    systemctl disable hostapd
    systemctl stop dhcpcd
    systemctl disable dhcpcd
    systemctl stop dhcpcd5
    systemctl disable dhcpcd5
    systemctl stop systemd-hostnamed
    systemctl disable systemd-hostnamed
    systemctl stop systemd-networkd
    systemctl disable systemd-networkd

    # setup iptables
    if [ ! -d /etc/sysconfig ]; then
        mkdir /etc/sysconfig
    fi

    if test -f /etc/sysconfig/iptables; then
        mv /etc/sysconfig/iptables /etc/sysconfig/iptables.kurasave
    fi
    sed -i "s|KURA_DIR|${BASE_DIR}/${KURA_SYMLINK}|" /lib/systemd/system/firewall.service
    cp -p /proc/sys/net/ipv4/ip_forward "${BASE_DIR}/${KURA_SYMLINK}/.data/ip_forward.kurasave"

    # disables cloud-init if exists and allows interface management to network-manager
    if [ -d /etc/cloud/cloud.cfg.d ]; then
        echo "network: {config: disabled}" | sudo tee -a /etc/cloud/cloud.cfg.d/99-disable-network-config.cfg > /dev/null
    fi
    if [ -d /usr/lib/NetworkManager/conf.d/ ]; then
        TO_REMOVE=$( find /usr/lib/NetworkManager/conf.d/ -type f -name  "*-globally-managed-devices.conf" | awk 'NR==1{print $1}' )
        
        if [ -f "${TO_REMOVE}" ]; then
            rm "${TO_REMOVE}"
        fi
    fi
    # comment network interface configurations in interfaces file
    if python3 -V > /dev/null 2>&1
    then
        python3 "${BASE_DIR}/${KURA_SYMLINK}/install/comment_interfaces_file.py"
    else
        echo "python3 not found. Please manually review the /etc/network/interfaces file and comment configured network interfaces."
    fi
    
    # install dnsmasq default configuration
    if [ -f /etc/default/dnsmasq ]; then
        mv /etc/default/dnsmasq /etc/default/dnsmasq.kurasave
    fi
    cp "${BASE_DIR}/${KURA_SYMLINK}/install/dnsmasq" /etc/default/dnsmasq

    install_named_config

    disable_netplan
}

setup_dnsmasq_conf_file() {
    
    DNSMASQ_CONF_FILE="/etc/dnsmasq.conf"
    if [ -f "${DNSMASQ_CONF_FILE}" ]; then
        if [ ! -f "${DNSMASQ_CONF_FILE}.kurasave" ]; then
            cp -f "${DNSMASQ_CONF_FILE}" "${DNSMASQ_CONF_FILE}.kurasave"
        fi
        sed -i s/^dhcp-/#dhcp-/g "${DNSMASQ_CONF_FILE}"
    fi

    DNSMASQ_LXC_FILE="/etc/dnsmasq.d/lxc"
    if [ -f "${DNSMASQ_LXC_FILE}" ]; then
        if [ ! -f "/etc/lxc.kurasave" ]; then
            cp -f "${DNSMASQ_LXC_FILE}" "/etc/lxc.kurasave"
        fi
        sed -i s/^bind-interfaces/bind-dynamic/g "${DNSMASQ_LXC_FILE}"
    fi
}

stop_and_disable_services() {
    for service in ${SERVICES_TO_STOP_AND_DISABLE}; do
        echo "Stopping and disabling $service."
        systemctl stop "${service}" &> /dev/null
        systemctl disable "${service}" &> /dev/null
    done
}

should_disable_systemd_resolved_stub() {
    if [ -x "$(command -v systemd)" ]; then

        SYSTEMD_VERSION=$(systemd --version | (IFS=" " read -r _ignore SYSTEMD_VERSION _ignore; echo "${SYSTEMD_VERSION}") || true)

        [ "${SYSTEMD_VERSION}" -lt 248 ] && 
        [ -L /etc/resolv.conf ] && 
        grep "^nameserver[ ]\+127.0.0.53" < /etc/resolv.conf > /dev/null 2>&1 && 
        [ -e /run/systemd/resolve/resolv.conf ]
    else
        false
    fi
}

setup_web_ui_kura_properties() {
    if [ -f "${BASE_DIR}/${KURA_SYMLINK}/framework/kura.properties" ]; then
        sed -i "s|^kura.have.net.admin=.*|kura.have.net.admin=true|" "${BASE_DIR}/${KURA_SYMLINK}/framework/kura.properties"
    fi
}

kura_install() {
    echo "Installing Kura networking..."

    cp /lib/systemd/system/kura.service /lib/systemd/system/kura.service.kurasave
    sed "s|INSTALL_DIR|${INSTALL_DIR}|" ${INSTALL_DIR}/kura/install/kura.service > /lib/systemd/system/kura.service
    systemctl daemon-reload
    systemctl enable kura

    stop_and_disable_services

    echo "Configuring Kura networking..."

    chmod 700 "${BASE_DIR}/${KURA_SYMLINK}/.data/manage_network_permissions.sh"

    if ! ( "${BASE_DIR}/${KURA_SYMLINK}/.data/manage_network_permissions.sh" -i ); then
        error_state "Error installing network permissions."
    fi

    setup_network_manager

    bash "${BASE_DIR}/${KURA_SYMLINK}/install/customize-installation.sh" "install" true

    setup_web_ui_kura_properties


    if should_disable_systemd_resolved_stub; then
        ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf
    fi

    setup_dnsmasq_conf_file

    rm -r "${BASE_DIR}/${KURA_SYMLINK}/install"

    # flush all cached filesystem to disk
    sync

    echo "Finished."
}

run_kura_network_install() {
    set -m
    kura_install &
    set +m
}

SYSTEM=$(ls -all /sbin/init);
if  ! echo "${SYSTEM}" | grep -qe "systemd" ; then
    error_state "Incompatible system version ${SYSTEM##* }"
fi

# check if java or keytool are not found
if (! java -version > /dev/null 2>&1) || (! keytool > /dev/null 2>&1); then
    # check if JVM path is not set
    if [ -z "${JAVA_HOME}" ]; then
        # retrieve java home using the link
        JAVA_HOME=$(readlink $(which java) | rev | cut -d"/" -f3- | rev)
    fi

    if [ -n "${JAVA_HOME}" ]; then
        # create a script that exports the variables at startup in /etc/profile.d
        touch /etc/profile.d/java_paths.sh
        echo "export JAVA_HOME=$JAVA_HOME" > /etc/profile.d/java_paths.sh

        # check if java bin already exists in PATH
        if [[ ":$PATH:" != *":$JAVA_HOME/bin:"* ]]; then
            echo "export PATH=$PATH:$JAVA_HOME/bin" >> /etc/profile.d/java_paths.sh
        fi

        # export the variables just for this session 
        source /etc/profile.d/java_paths.sh
    else
        error_state "Java binary cannot be found."
    fi
fi

run_kura_network_install

exit 0
