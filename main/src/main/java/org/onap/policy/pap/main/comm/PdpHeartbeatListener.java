/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.comm;

import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.parameters.PdpParameters;

/**
 * Listener for PDP Status messages which either represent registration or heart beat.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpHeartbeatListener implements TypedMessageListener<PdpStatus> {

    private final PdpParameters params;

    /**
     * Constructs the object.
     *
     * @param params PDP parameters
     */
    public PdpHeartbeatListener(PdpParameters params) {
        this.params = params;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final PdpStatus message) {

        final var handler = new PdpStatusMessageHandler(params);
        handler.handlePdpStatus(message);
    }
}
