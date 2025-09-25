#!/usr/bin/env python3
#
# Copyright (c) 2024, 2025 Eurotech and/or its affiliates
#
#  All rights reserved.
#
import sys
import logging
import os
import os.path
import argparse

sys.path.append("/opt/eclipse/kura/.data")

from network_tools import get_eth_wlan_interfaces_names

# Define constants at the top of the file
TEMPLATE_DIR = "/opt/eclipse/kura/kura-networking-install"
TEMPLATES = {
    "firewall": os.path.join(TEMPLATE_DIR, "template_firewall_eth"),
    "firewall_wlan" : os.path.join(TEMPLATE_DIR, "template_firewall_eth_wlan"),
    "flooding": os.path.join(TEMPLATE_DIR, "template_flooding"),
    "multiple_eth_no_wlan" : os.path.join(TEMPLATE_DIR, "template_multiple_eth_no_wlan"),
    "multiple_eth_one_wlan" : os.path.join(TEMPLATE_DIR, "template_multiple_eth_one_wlan"),
    "one_eth_no_wlan" : os.path.join(TEMPLATE_DIR, "template_one_eth_no_wlan"),
    "one_eth_one_wlan" : os.path.join(TEMPLATE_DIR, "template_one_eth_one_wlan")
}

def validate_file_path(file_path):
    # Get absolute path
    abs_path = os.path.abspath(file_path)
    # Define allowed directories
    allowed_dirs = [
        "/opt/eclipse/kura/user/snapshots/",
        "/opt/eclipse/kura/.data/"
    ]
    # Check if file path is within any of the allowed directories
    for allowed_dir in allowed_dirs:
        if abs_path.startswith(allowed_dir):
            return abs_path
    raise ValueError(f"File path not allowed: {abs_path}")

def read_file_safely(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    except (IOError, PermissionError) as e:
        logging.error("Error reading file %s: %s", file_path, str(e))
        raise

def write_file_safely(file_path, content):
    try:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
    except (IOError, PermissionError) as e:
        logging.error("Error writing file %s: %s", file_path, str(e))
        raise

# Validate template existence
def validate_templates():
    for name, path in TEMPLATES.items():
        if not os.path.isfile(path):
            logging.error("Template file missing: %s", path)
            return False
    return True

def main():
    logging.basicConfig(
        format='[customize_snapshot_network.py] %(asctime)s %(levelname)s %(message)s',
        level=logging.INFO,
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.StreamHandler()
        ]
    )

    parser = argparse.ArgumentParser(description="Customize snapshot_0.xml file", usage='%(prog)s snapshot_filename')
    parser.add_argument('snapshot_filename', help='The path of the snapshot_0.xml file')
    args = parser.parse_args()

    try:
        safe_path = validate_file_path(args.snapshot_filename)
    except ValueError as e:
        logging.error("Invalid file path: %s", str(e))
        sys.exit(1)

    (eth_names, wlan_names) = get_eth_wlan_interfaces_names()

    eth_number = len(eth_names)
    wlan_number = len(wlan_names)

    if eth_number == 0:
        logging.info("ERROR: no ethernet interface found")
        exit(1)

    if not validate_templates():
        logging.error("Template validation failed")
        exit(1)

    firewall_configuration_template = TEMPLATES["firewall"]
    network_configuration_template = TEMPLATES["one_eth_no_wlan"]
    flooding_configuration_template = TEMPLATES["flooding"]

    if eth_number == 1:
        if wlan_number == 0:
            network_configuration_template = TEMPLATES["one_eth_no_wlan"]
            firewall_configuration_template = TEMPLATES["firewall_eth"]
        else:
            network_configuration_template = TEMPLATES["one_eth_one_wlan"]
            firewall_configuration_template = TEMPLATES["firewall_wlan"]
    else:
        if wlan_number == 0:
            network_configuration_template = TEMPLATES["multiple_eth_no_wlan"]
            firewall_configuration_template = TEMPLATES["firewall_eth"]
        else:
            network_configuration_template = TEMPLATES["multiple_eth_one_wlan"]
            firewall_configuration_template = TEMPLATES["firewall_wlan"]


    logging.info("%s : starting editing", safe_path)
    snapshot_content = read_file_safely(safe_path)

    network_template_content = read_file_safely(network_configuration_template)

    firewall_template_content = read_file_safely(firewall_configuration_template)
    flooding_template_content = read_file_safely(flooding_configuration_template)

    closing_tag = '</esf:configurations>'
    insert_position = snapshot_content.rfind(closing_tag)

    # Check if services are already present in the snapshot content
    network_present = "NetworkConfigurationService" in snapshot_content
    firewall_present = "FirewallConfigurationService" in snapshot_content
    flooding_present = "FloodingProtectionConfigurator" in snapshot_content

    inserted_content = ""
    if not network_present:
        inserted_content += network_template_content + "\n"
        logging.info("%s : adding network configuration", safe_path)
    else:
        logging.info("%s : network configuration already present, skipping", safe_path)

    if not firewall_present:
        inserted_content += firewall_template_content + "\n"
        logging.info("%s : adding firewall configuration", safe_path)
    else:
        logging.info("%s : firewall configuration already present, skipping", safe_path)

    if not flooding_present:
        inserted_content += flooding_template_content + "\n"
        logging.info("%s : adding flooding protection configuration", safe_path)
    else:
        logging.info("%s : flooding protection configuration already present, skipping", safe_path)

    snapshot_content = snapshot_content[:insert_position] + inserted_content + snapshot_content[insert_position:]

    interfaces_list = "lo"

    for i, eth_name in enumerate(eth_names[:2]):
        snapshot_content = snapshot_content.replace('ETH_INTERFACE_' + str(i), eth_name)
        interfaces_list += "," + eth_name
        logging.info("%s : replaced ETH_INTERFACE_%s with %s", safe_path, str(i), eth_name)

    for i, wlan_name in enumerate(wlan_names[:1]):
        snapshot_content = snapshot_content.replace('WIFI_INTERFACE_' + str(i), wlan_name)
        interfaces_list += "," + wlan_name
        logging.info("%s : replaced WIFI_INTERFACE_%s with %s", safe_path, str(i), wlan_name)

    snapshot_content = snapshot_content.replace('INTERFACES_LIST', interfaces_list)

    write_file_safely(safe_path, snapshot_content)

    logging.info("%s : successfully edited", safe_path)

if __name__ == "__main__":
    main()
