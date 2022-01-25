/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.repository;

import java.util.List;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.pdp.persistence.concepts.JpaPdpGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdpGroupRepository extends JpaRepository<JpaPdpGroup, PfConceptKey> {

    public List<JpaPdpGroup> findByKeyName(String pdpGroup);

    public List<JpaPdpGroup> findByPdpGroupState(PdpState pdpState);

    public List<JpaPdpGroup> findByKeyNameAndPdpGroupState(String pdpGroup, PdpState pdpState);
}
