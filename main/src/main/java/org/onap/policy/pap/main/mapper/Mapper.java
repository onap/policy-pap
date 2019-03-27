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

package org.onap.policy.pap.main.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpInstanceDetails;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PolicyIdent;
import org.onap.policy.models.pdp.concepts.PolicyIdentOptVersion;
import org.onap.policy.models.pdp.concepts.PolicyTypeIdent;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;

/**
 * Classes used to map internal PAP/PDP classes to/from external classes used by the PAP
 * REST API.
 */
public class Mapper {

    private Mapper() {
        super();
    }


    // these methods map from internal to external

    /**
     * Converts an internal PdpGroups to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PdpGroups toExternal(PdpGroups source) {

        org.onap.policy.models.pap.concepts.PdpGroups target = new org.onap.policy.models.pap.concepts.PdpGroups();

        target.setGroups(mapIt(source.getGroups(), Mapper::toExternal));

        return target;
    }

    /**
     * Converts an internal PdpGroup to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PdpGroup toExternal(PdpGroup source) {

        org.onap.policy.models.pap.concepts.PdpGroup target = new org.onap.policy.models.pap.concepts.PdpGroup();

        target.setDescription(source.getDescription());
        target.setName(IdentUtil.nameToExternal(source.getKey().getName()));
        target.setPdpGroupState(source.getPdpGroupState());
        target.setPdpSubgroups(mapIt(source.getPdpSubgroups(), Mapper::toExternal));
        target.setProperties(mapIt(source.getProperties()));
        target.setVersion(IdentUtil.versionToExternal(source.getKey().getVersion()));

        return target;
    }

    /**
     * Converts an internal PdpSubGroup to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PdpSubGroup toExternal(PdpSubGroup source) {

        org.onap.policy.models.pap.concepts.PdpSubGroup target = new org.onap.policy.models.pap.concepts.PdpSubGroup();

        target.setCurrentInstanceCount(source.getCurrentInstanceCount());
        target.setDesiredInstanceCount(source.getDesiredInstanceCount());
        target.setPdpInstances(mapIt(source.getPdpInstances(), Mapper::toExternal));
        target.setPdpType(source.getPdpType());
        target.setPolicies(mapIt(source.getPolicies(), Mapper::toExternal));
        target.setProperties(mapIt(source.getProperties()));
        target.setSupportedPolicyTypes(mapIt(source.getSupportedPolicyTypes(), Mapper::toExternal));

        return target;
    }

    /**
     * Converts an internal policy to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.Policy toExternal(ToscaPolicy source) {

        org.onap.policy.models.pap.concepts.Policy target = new org.onap.policy.models.pap.concepts.Policy();

        target.setName(source.getKey().getName());
        target.setPolicyVersion(source.getKey().getVersion());
        target.setPolicyType(source.getType().getName());
        target.setPolicyTypeVersion(source.getType().getVersion());

        // TODO setPolicyTypeImpl

        return target;
    }

    /**
     * Converts an internal PDP instance details to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PdpInstanceDetails toExternal(PdpInstanceDetails source) {

        org.onap.policy.models.pap.concepts.PdpInstanceDetails target =
                        new org.onap.policy.models.pap.concepts.PdpInstanceDetails();

        target.setHealthy(source.getHealthy());
        target.setInstanceId(source.getInstanceId());
        target.setMessage(source.getMessage());
        target.setPdpState(source.getPdpState());

        return target;
    }

    /**
     * Converts an internal policy identifier to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PolicyIdent toExternal(PolicyIdent source) {

        org.onap.policy.models.pap.concepts.PolicyIdent target = new org.onap.policy.models.pap.concepts.PolicyIdent();

        target.setName(IdentUtil.nameToExternal(source.getName()));
        target.setVersion(IdentUtil.versionToExternal(source.getVersion()));

        return target;
    }

    /**
     * Converts an internal policy type identifier to the corresponding external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PolicyTypeIdent toExternal(PolicyTypeIdent source) {

        org.onap.policy.models.pap.concepts.PolicyTypeIdent target =
                        new org.onap.policy.models.pap.concepts.PolicyTypeIdent();

        target.setName(IdentUtil.nameToExternal(source.getName()));
        target.setVersion(IdentUtil.versionToExternal(source.getVersion()));

        return target;
    }

    /**
     * Converts an internal policy identifier, with optional version, to the corresponding
     * external class.
     *
     * @return an external object, populated with data from this object
     */
    public static org.onap.policy.models.pap.concepts.PolicyIdentOptVersion toExternal(PolicyIdentOptVersion source) {

        org.onap.policy.models.pap.concepts.PolicyIdentOptVersion target =
                        new org.onap.policy.models.pap.concepts.PolicyIdentOptVersion();

        target.setName(IdentUtil.nameToExternal(source.getName()));
        target.setVersion(IdentUtil.versionToExternal(source.getVersion()));

        return target;
    }


    // these methods map from external to internal

    /**
     * Converts an external PDP instance details to the corresponding internal class.
     *
     * @return an internal object, populated with data from this object
     */
    public static PdpInstanceDetails toInternal(org.onap.policy.models.pap.concepts.PdpInstanceDetails source) {
        PdpInstanceDetails target = new PdpInstanceDetails();

        target.setHealthy(source.getHealthy());
        target.setInstanceId(source.getInstanceId());
        target.setMessage(source.getMessage());
        target.setPdpState(source.getPdpState());

        return target;
    }

    /**
     * Converts an external policy identifier to the corresponding internal class.
     *
     * @return an internal object, populated with data from this object
     */
    public static PolicyIdent toInternal(org.onap.policy.models.pap.concepts.PolicyIdent source) {
        PolicyIdent target = new PolicyIdent();

        IdentUtil.toInternal(source.getName(), source.getVersion(), target);

        return target;
    }

    /**
     * Converts an external policy type identifier to the corresponding internal class.
     *
     * @return an internal object, populated with data from this object
     */
    public static PolicyTypeIdent toInternal(org.onap.policy.models.pap.concepts.PolicyTypeIdent source) {
        PolicyTypeIdent target = new PolicyTypeIdent();

        IdentUtil.toInternal(source.getName(), source.getVersion(), target);

        return target;
    }

    /**
     * Converts an external policy type identifier, with optional version, to the
     * corresponding internal class.
     *
     * @return an internal object, populated with data from this object
     */
    public static PolicyIdentOptVersion toInternal(org.onap.policy.models.pap.concepts.PolicyIdentOptVersion source) {
        PolicyIdentOptVersion target = new PolicyIdentOptVersion();

        IdentUtil.toInternal(source.getName(), source.getVersion(), target);

        return target;
    }

    /**
     * Maps a list of one type of object to a list of another type of object.
     *
     * @param source list of items to be mapped
     * @param mapper function to map an item to an object of the target type
     * @return a list of objects of the new type
     */
    private static <T, R> List<R> mapIt(List<T> source, Function<T, R> mapper) {
        return source.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Creates a copy of a map.
     *
     * @param map map to be copied
     * @return a copy of the map
     */
    private static Map<String, String> mapIt(Map<String, String> map) {
        return new LinkedHashMap<>(map);
    }
}
