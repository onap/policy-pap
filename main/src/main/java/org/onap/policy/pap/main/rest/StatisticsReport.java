/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class to represent statistics report of pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@ToString
public class StatisticsReport {

    @Getter
    @Setter
    private int code;
    @Getter
    @Setter
    private long totalPdpCount;
    @Getter
    @Setter
    private long totalPdpGroupCount;
    @Getter
    @Setter
    private long totalPolicyDeployCount;
    @Getter
    @Setter
    private long policyDeploySuccessCount;
    @Getter
    @Setter
    private long policyDeployFailureCount;
    @Getter
    @Setter
    private long totalPolicyDownloadCount;
    @Getter
    @Setter
    private long policyDownloadSuccessCount;
    @Getter
    @Setter
    private long policyDownloadFailureCount;
}
