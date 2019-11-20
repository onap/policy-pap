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

package org.onap.policy.pap.main.comm.msgdata;

import java.util.Collection;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.notification.PolicyNotifier;

/**
 * Request data, whose message may be changed at any point, possibly triggering a restart
 * of the publishing.
 */
public interface Request {

    /**
     * Gets the name with which this data is associated, used for logging purposes. This
     * may be changed when this is reconfigured.
     *
     * @return the name with which this data is associated
     */
    public String getName();

    /**
     * Gets the current message.
     *
     * @return the current message
     */
    public PdpMessage getMessage();

    /**
     * Sets the listener that will receive request events.
     *
     * @param listener the request listener
     */
    public void setListener(RequestListener listener);

    /**
     * Sets the notifier to track responses to the request.
     *
     * @param notifier notifier used to publish notifications
     */
    public void setNotifier(PolicyNotifier notifier);

    /**
     * Determines if this request is currently being published.
     *
     * @return {@code true} if this request is being published, {@code false} otherwise
     */
    public boolean isPublishing();

    /**
     * Starts the publishing process, registering any listeners or timeout handlers, and
     * adding the request to the publisher queue.
     */
    public void startPublishing();

    /**
     * Unregisters the listener, cancels the timer, and removes the message from the
     * queue.
     */
    public void stopPublishing();

    /**
     * Reconfigures the fields based on the {@link #message} type. Suspends publishing,
     * updates the configuration, and then resumes publishing.
     *
     * @param newMessage the new message
     * @return {@code true} if reconfiguration was successful, {@code false} otherwise
     */
    public boolean reconfigure(PdpMessage newMessage);

    /**
     * Checks the response to ensure it is as expected.
     *
     * @param response the response to check
     * @return an error message, if a fatal error has occurred, {@code null} otherwise
     */
    public String checkResponse(PdpStatus response);

    /**
     * If a request fails, this gets a list of the policies that should be undeployed.
     *
     * @return a list of policies to be undeployed
     */
    public Collection<ToscaPolicyIdentifier> getUndeployPolicies();
}
