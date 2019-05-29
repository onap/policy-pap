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
docker run -p 9090:9090 -p 6969:6969 -v ${DIR}/config/pap/bin/policy-pap.sh:/opt/app/policy/pap/bin/policy-pap.sh -v ${DIR}/config/pap/etc/topic.properties:/opt/app/policy/pap/etc/topic.properties --add-host mariadb:10.2.0.41 --name policy-pap -d --rm nexus3.onap.org:10001/onap/policy-pap:2.0.0-SNAPSHOT-latest
