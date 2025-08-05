#!/bin/bash

#
# CONFIGURATION FILE FOR THE INSTALLATION PROCEDURE
#

#
# Root for the old logs directory, do not end the path with "/"
#
OLD_LOGS_ROOT="/var/log/kura_old_logs"

#
# List of services that will be stopped and disabled prior the installation procedure
#
SERVICES_TO_STOP_AND_DISABLE="
systemd-timesyncd
chronyd
chrony
ntpd
"

#
# Execute custom commands at the end of the installation process
#
execute_custom_install() {
    echo "Executing customizations."
}

#
# Execute custom commands at the end of the uninstallation process
#
execute_custom_uninstall() {
    echo "Removing customizations."
}
