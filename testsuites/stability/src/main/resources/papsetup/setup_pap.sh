#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (c) 2019 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================

# the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo ${DIR}

if ["$#" -lt 2]; then
	echo "PAP and MariaDB IPs should be passed as two parameters. PAP IP goes first."
	exit 1
else
    PAP=$1
    echo "PAP IP: ${PAP}"
    MARIADB=$2
    echo "MariaDB IP: ${MARIADB}"
fi

docker run -p 9090:9090 -p 6969:6969 -e "PAP_HOST=${PAP}" -v ${DIR}/config/pap/bin/policy-pap.sh:/opt/app/policy/pap/bin/policy-pap.sh -v ${DIR}/config/pap/etc/defaultConfig.json:/opt/app/policy/pap/etc/defaultConfig.json --add-host mariadb:${MARIADB} --name policy-pap -d --rm nexus3.onap.org:10001/onap/policy-pap:2.0.0-SNAPSHOT-latest
