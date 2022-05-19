#!/usr/bin/env sh
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019-2022 Nordix Foundation.
#  Modifications Copyright (C) 2019-2021 AT&T Intellectual Property.
#  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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
#

KEYSTORE="${KEYSTORE:-$POLICY_HOME/etc/ssl/policy-keystore}"
TRUSTSTORE="${TRUSTSTORE:-$POLICY_HOME/etc/ssl/policy-truststore}"
KEYSTORE_PASSWD="${KEYSTORE_PASSWD:-Pol1cy_0nap}"
TRUSTSTORE_PASSWD="${TRUSTSTORE_PASSWD:-Pol1cy_0nap}"

if [ "$#" -ge 1 ]; then
    CONFIG_FILE=$1
else
    CONFIG_FILE=${CONFIG_FILE}
fi

touch /app/pap.jar

if [ -z "$CONFIG_FILE" ]; then
    CONFIG_FILE="${POLICY_HOME}/etc/papParameters.yaml"
fi

echo "Policy pap config file: $CONFIG_FILE"

if [ -f "${POLICY_HOME}/etc/mounted/policy-truststore" ]; then
    echo "overriding policy-truststore"
    cp -f "${POLICY_HOME}"/etc/mounted/policy-truststore "${TRUSTSTORE}"
fi

if [ -f "${POLICY_HOME}/etc/mounted/policy-keystore" ]; then
    echo "overriding policy-keystore"
    cp -f "${POLICY_HOME}"/etc/mounted/policy-keystore "${KEYSTORE}"
fi

if [ -f "${POLICY_HOME}/etc/mounted/logback.xml" ]; then
    echo "overriding logback.xml"
    cp -f "${POLICY_HOME}"/etc/mounted/logback.xml "${POLICY_HOME}"/etc/
fi

# provide and external PDP group configuration if a groups.json
# file is present in the data directory. If none is present,
# the PAP will use the PapDb.json resource in the classpath
# to load a default group.

if [ -f "${POLICY_HOME}/etc/mounted/groups.json" ]; then
    CUSTOM_GROUPS="${POLICY_HOME}/etc/mounted/groups.json"
else
    CUSTOM_GROUPS="PapDb.json"
fi

echo "PDP group configuration file: ${CUSTOM_GROUPS}"

$JAVA_HOME/bin/java \
    -Dlogging.config="${POLICY_HOME}/etc/logback.xml" \
    -Dserver.ssl.key-store="${KEYSTORE}" \
    -Dserver.ssl.key-store-password="${KEYSTORE_PASSWD}" \
    -Djavax.net.ssl.trustStore="${TRUSTSTORE}" \
    -Djavax.net.ssl.trustStorePassword="${TRUSTSTORE_PASSWD}" \
    -jar /app/pap.jar \
    --spring.config.location="${CONFIG_FILE}" \
    --group-config-file="${CUSTOM_GROUPS}"
