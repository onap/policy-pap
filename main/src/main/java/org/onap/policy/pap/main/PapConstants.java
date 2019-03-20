/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main;

/**
 * Names of various items contained in the Registry.
 */
public class PapConstants {

    // Registry keys
    public static final String REG_PAP_ACTIVATOR = "object:activator/pap";
    public static final String REG_STATISTICS_MANAGER = "object:manager/statistics";
    public static final String REG_PDP_PARAMETERS = "parameter:pdp";
    public static final String REG_PDP_PUBLISHER = "object:publisher/pdp";
    public static final String REG_PDP_UPDATE_TIMERS = "timer:manager:pdp/update";
    public static final String REG_PDP_STATE_CHANGE_TIMERS = "timer:manager:pdp/state/change";
    public static final String REG_PDP_RESPONSE_DISPATCHER = "listener:pdp/dispatcher";
    public static final String REG_PDP_MODIFY_LOCK = "lock:pdp";
    public static final String REG_PDP_MODIFY_MAP = "object:pdp/modify/map";

    // topic names
    public static final String TOPIC_POLICY_PDP_PAP = "POLICY-PDP-PAP";

    private PapConstants() {
        super();
    }
}
