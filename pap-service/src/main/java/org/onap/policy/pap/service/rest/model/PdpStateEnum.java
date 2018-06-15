/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.pap.service.rest.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the possible states of a PDP.
 */
public enum PdpStateEnum {
    @SerializedName("passive")
    PASSIVE("passive"),

    @SerializedName("active")
    ACTIVE("active"),

    @SerializedName("safe")
    SAFE("safe"),

    @SerializedName("test")
    TEST("test");

    private String value;

    PdpStateEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Convert from String value to PdpStateEnum.
     * 
     * @param text the String value
     * @return the corresponding PdpStateEnum
     */
    public static PdpStateEnum fromValue(String text) {
        for (PdpStateEnum b : PdpStateEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
