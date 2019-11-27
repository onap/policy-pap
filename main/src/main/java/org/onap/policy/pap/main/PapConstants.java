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
    public static final String REG_PDP_MODIFY_LOCK = "lock:pdp";
    public static final String REG_PDP_MODIFY_MAP = "object:pdp/modify/map";
    public static final String REG_PDP_TRACKER = "object:pdp/tracker";
    public static final String REG_POLICY_NOTIFIER = "object:policy/notifier";
    public static final String REG_PAP_DAO_FACTORY = "object:pap/dao/factory";

    // topic names
    public static final String TOPIC_POLICY_PDP_PAP = "POLICY-PDP-PAP";
    public static final String TOPIC_POLICY_NOTIFICATION = "POLICY-NOTIFICATION";

    // policy components names
    public static final String POLICY_API = "Policy API";
    public static final String POLICY_DISTRIBUTION = "Policy Distribution";
    public static final String POLICY_PAP = "Policy PAP";
    public static final String POLICY_PDPS = "Policy PDPs";

    private PapConstants() {
        super();
    }
}
