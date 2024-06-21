#
# ============LICENSE_START=================================================
# Copyright (C) 2024 Nordix Foundation.
# ==========================================================================
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
# ============LICENSE_END===================================================
#

CURRENT_DIR=$(git rev-parse --show-toplevel)

# build an image locally, skipping tests (that's for java-verify job)
mvn clean install -P docker -DskipTests

# lookup version in the pom.xml
XML_FILE=${CURRENT_DIR}/main/pom.xml
TAG="version"
VERSION=$(grep -oP "(?<=<$TAG>).*?(?=</$TAG>)" "$XML_FILE" | head -n 1)

# tag the newly built image to nexus image
docker tag onap/policy-pap:${VERSION} nexus3.onap.org:10001/onap/policy-pap:${VERSION}

# clone policy-docker to run the tests
GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' "${CURRENT_DIR}"/.gitreview)
git clone -b "${GERRIT_BRANCH}" --single-branch https://github.com/onap/policy-docker.git

cd "${CURRENT_DIR}"/policy-docker/csit
bash run-project-csit.sh pap
RC=$?

# clean up
cd "${CURRENT_DIR}"
rm -rf policy-docker/

exit RC
