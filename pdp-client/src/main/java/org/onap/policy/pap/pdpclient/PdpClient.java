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

package org.onap.policy.pap.pdpclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.onap.policy.model.pdpclient.ModeParameters;
import org.onap.policy.model.pdpclient.Policy;
import org.onap.policy.model.pdpclient.PolicyActivationParameters;
import org.onap.policy.model.pdpclient.PolicyRetirementParameters;
import org.onap.policy.model.pdpclient.PolicyRollbackParameters;
import org.onap.policy.model.pdpclient.PolicyUpgradeParameters;
import org.onap.policy.swaggerclient.ApiCallback;
import org.onap.policy.swaggerclient.ApiClient;
import org.onap.policy.swaggerclient.ApiException;
import org.onap.policy.swaggerclient.ApiResponse;
import org.onap.policy.swaggerclient.ProgressRequestBody;
import org.onap.policy.swaggerclient.ProgressResponseBody;

public class PdpClient {
    private static final String POLICY_DEPLOYMENT_UPGRADE = "/policyDeployment/upgrade";
    private static final String POLICY_DEPLOYMENT_ROLLBACK = "/policyDeployment/rollback";
    private static final String POLICY_DEPLOYMENT_RETIRE = "/policyDeployment/retire";
    private static final String POLICY_DEPLOYMENT_MODE = "/policyDeployment/mode";
    private static final String POLICY_DEPLOYMENT_DEPLOY = "/policyDeployment/deploy";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ACCEPT = "Accept";
    private static final String POLICY_DEPLOYMENT_ACTIVATE = "/policyDeployment/activate";
    private ApiClient apiClient;

    public PdpClient(String basePath) {
        this.apiClient = new ApiClient(basePath);
    }

    /**
     * Build call for policyDeploymentActivatePut.
     * 
     * @param policyActivationParameters The parameters defining the activation (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call policyDeploymentActivatePutCall(
            PolicyActivationParameters policyActivationParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Map<String, String> localVarHeaderParams = new HashMap<>();
        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put(ACCEPT, localVarAccept);
        }

        final String[] localVarContentTypes = {APPLICATION_JSON};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put(CONTENT_TYPE, localVarContentType);

        if (progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain)
                        throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(POLICY_DEPLOYMENT_ACTIVATE, "PUT", new ArrayList<>(), new ArrayList<>(),
                policyActivationParameters, localVarHeaderParams, new HashMap<>(), localVarAuthNames,
                progressRequestListener);
    }

    private com.squareup.okhttp.Call policyDeploymentActivatePutValidateBeforeCall(
            PolicyActivationParameters policyActivationParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {


        return policyDeploymentActivatePutCall(policyActivationParameters, progressListener, progressRequestListener);

    }

    /**
     * Activate a policy on the PDP
     * 
     * @param policyActivationParameters The parameters defining the activation (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public void policyDeploymentActivatePut(PolicyActivationParameters policyActivationParameters) throws ApiException {
        policyDeploymentActivatePutWithHttpInfo(policyActivationParameters);
    }

    /**
     * Activate a policy on the PDP
     * 
     * @param policyActivationParameters The parameters defining the activation (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public ApiResponse<Void> policyDeploymentActivatePutWithHttpInfo(
            PolicyActivationParameters policyActivationParameters) throws ApiException {
        com.squareup.okhttp.Call call =
                policyDeploymentActivatePutValidateBeforeCall(policyActivationParameters, null, null);
        return apiClient.execute(call);
    }

    /**
     * Activate a policy on the PDP (asynchronously)
     * 
     * @param policyActivationParameters The parameters defining the activation (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body
     *         object
     */
    public com.squareup.okhttp.Call policyDeploymentActivatePutAsync(
            PolicyActivationParameters policyActivationParameters, final ApiCallback<Void> callback)
            throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = callback::onDownloadProgress;
            progressRequestListener = callback::onUploadProgress;
        }

        com.squareup.okhttp.Call call = policyDeploymentActivatePutValidateBeforeCall(policyActivationParameters,
                progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }

    /**
     * Build call for policyDeploymentDeployPut.
     * 
     * @param policy The policy to deploy
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call policyDeploymentDeployPutCall(Policy policy,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Map<String, String> localVarHeaderParams = new HashMap<>();

        Map<String, Object> localVarFormParams = new HashMap<>();
        localVarFormParams.put("file", policy.getPolicyFiles().get(0));
        localVarFormParams.put("policyName", policy.getPolicyName());
        localVarFormParams.put("policyVersion", policy.getPolicyVersion());

        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put(ACCEPT, localVarAccept);
        }

        final String[] localVarContentTypes = {"multipart/form-data"};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put(CONTENT_TYPE, localVarContentType);

        if (progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain)
                        throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(POLICY_DEPLOYMENT_DEPLOY, "PUT", new ArrayList<>(), new ArrayList<>(), null,
                localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    private com.squareup.okhttp.Call policyDeploymentDeployPutValidateBeforeCall(Policy policyDeploymentParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        return policyDeploymentDeployPutCall(policyDeploymentParameters, progressListener, progressRequestListener);

    }

    /**
     * Deploys a policy to the PDP
     * 
     * @param policyDeploymentParameters The parameters defining the deployment (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public void policyDeploymentDeployPut(Policy policyDeploymentParameters) throws ApiException {
        policyDeploymentDeployPutWithHttpInfo(policyDeploymentParameters);
    }

    /**
     * Deploys a policy to the PDP
     * 
     * @param policyDeploymentParameters The parameters defining the deployment (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public ApiResponse<Void> policyDeploymentDeployPutWithHttpInfo(Policy policyDeploymentParameters)
            throws ApiException {
        com.squareup.okhttp.Call call =
                policyDeploymentDeployPutValidateBeforeCall(policyDeploymentParameters, null, null);
        return apiClient.execute(call);
    }

    /**
     * Deploys a policy to the PDP (asynchronously)
     * 
     * @param policyDeploymentParameters The parameters defining the deployment (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body
     *         object
     */
    public com.squareup.okhttp.Call policyDeploymentDeployPutAsync(Policy policyDeploymentParameters,
            final ApiCallback<Void> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = callback::onDownloadProgress;
            progressRequestListener = callback::onUploadProgress;
        }

        com.squareup.okhttp.Call call = policyDeploymentDeployPutValidateBeforeCall(policyDeploymentParameters,
                progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }

    /**
     * Build call for policyDeploymentModePut.
     * 
     * @param modeParameters The parameters defining the mode (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call policyDeploymentModePutCall(ModeParameters modeParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Map<String, String> localVarHeaderParams = new HashMap<>();
        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put(ACCEPT, localVarAccept);
        }

        final String[] localVarContentTypes = {APPLICATION_JSON};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put(CONTENT_TYPE, localVarContentType);

        if (progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain)
                        throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(POLICY_DEPLOYMENT_MODE, "PUT", new ArrayList<>(), new ArrayList<>(), modeParameters,
                localVarHeaderParams, new HashMap<>(), localVarAuthNames, progressRequestListener);
    }

    private com.squareup.okhttp.Call policyDeploymentModePutValidateBeforeCall(ModeParameters modeParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        return policyDeploymentModePutCall(modeParameters, progressListener, progressRequestListener);
    }

    /**
     * Set the mode of the PDP
     * 
     * @param modeParameters The parameters defining the mode (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public void policyDeploymentModePut(ModeParameters modeParameters) throws ApiException {
        policyDeploymentModePutWithHttpInfo(modeParameters);
    }

    /**
     * Set the mode of the PDP
     * 
     * @param modeParameters The parameters defining the mode (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public ApiResponse<Void> policyDeploymentModePutWithHttpInfo(ModeParameters modeParameters) throws ApiException {
        com.squareup.okhttp.Call call = policyDeploymentModePutValidateBeforeCall(modeParameters, null, null);
        return apiClient.execute(call);
    }

    /**
     * Set the mode of the PDP (asynchronously)
     * 
     * @param modeParameters The parameters defining the mode (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body
     *         object
     */
    public com.squareup.okhttp.Call policyDeploymentModePutAsync(ModeParameters modeParameters,
            final ApiCallback<Void> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = callback::onDownloadProgress;
            progressRequestListener = callback::onUploadProgress;
        }

        com.squareup.okhttp.Call call =
                policyDeploymentModePutValidateBeforeCall(modeParameters, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }

    /**
     * Build call for policyDeploymentRetirePut.
     * 
     * @param policyRetirementParameters The parameters defining the retirement (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call policyDeploymentRetirePutCall(PolicyRetirementParameters policyRetirementParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Map<String, String> localVarHeaderParams = new HashMap<>();
        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put(ACCEPT, localVarAccept);
        }

        final String[] localVarContentTypes = {APPLICATION_JSON};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put(CONTENT_TYPE, localVarContentType);

        if (progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain)
                        throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(POLICY_DEPLOYMENT_RETIRE, "PUT", new ArrayList<>(), new ArrayList<>(),
                policyRetirementParameters, localVarHeaderParams, new HashMap<>(), localVarAuthNames,
                progressRequestListener);
    }

    private com.squareup.okhttp.Call policyDeploymentRetirePutValidateBeforeCall(
            PolicyRetirementParameters policyRetirementParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        return policyDeploymentRetirePutCall(policyRetirementParameters, progressListener, progressRequestListener);
    }

    /**
     * Retire a policy from the PDP
     * 
     * @param policyRetirementParameters The parameters defining the retirement (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public void policyDeploymentRetirePut(PolicyRetirementParameters policyRetirementParameters) throws ApiException {
        policyDeploymentRetirePutWithHttpInfo(policyRetirementParameters);
    }

    /**
     * Retire a policy from the PDP
     * 
     * @param policyRetirementParameters The parameters defining the retirement (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public ApiResponse<Void> policyDeploymentRetirePutWithHttpInfo(
            PolicyRetirementParameters policyRetirementParameters) throws ApiException {
        com.squareup.okhttp.Call call =
                policyDeploymentRetirePutValidateBeforeCall(policyRetirementParameters, null, null);
        return apiClient.execute(call);
    }

    /**
     * Retire a policy from the PDP (asynchronously)
     * 
     * @param policyRetirementParameters The parameters defining the retirement (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body
     *         object
     */
    public com.squareup.okhttp.Call policyDeploymentRetirePutAsync(
            PolicyRetirementParameters policyRetirementParameters, final ApiCallback<Void> callback)
            throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = callback::onDownloadProgress;
            progressRequestListener = callback::onUploadProgress;
        }

        com.squareup.okhttp.Call call = policyDeploymentRetirePutValidateBeforeCall(policyRetirementParameters,
                progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }

    /**
     * Build call for policyDeploymentRollbackPut.
     * 
     * @param policyRollbackParameters The parameters defining the rollback (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call policyDeploymentRollbackPutCall(PolicyRollbackParameters policyRollbackParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Map<String, String> localVarHeaderParams = new HashMap<>();
        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put(ACCEPT, localVarAccept);
        }

        final String[] localVarContentTypes = {APPLICATION_JSON};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put(CONTENT_TYPE, localVarContentType);

        if (progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain)
                        throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(POLICY_DEPLOYMENT_ROLLBACK, "PUT", new ArrayList<>(), new ArrayList<>(),
                policyRollbackParameters, localVarHeaderParams, new HashMap<>(), localVarAuthNames,
                progressRequestListener);
    }

    private com.squareup.okhttp.Call policyDeploymentRollbackPutValidateBeforeCall(
            PolicyRollbackParameters policyRollbackParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        return policyDeploymentRollbackPutCall(policyRollbackParameters, progressListener, progressRequestListener);
    }

    /**
     * Rollback a policy on the PDP to an older version
     * 
     * @param policyRollbackParameters The parameters defining the rollback (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public void policyDeploymentRollbackPut(PolicyRollbackParameters policyRollbackParameters) throws ApiException {
        policyDeploymentRollbackPutWithHttpInfo(policyRollbackParameters);
    }

    /**
     * Rollback a policy on the PDP to an older version
     * 
     * @param policyRollbackParameters The parameters defining the rollback (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public ApiResponse<Void> policyDeploymentRollbackPutWithHttpInfo(PolicyRollbackParameters policyRollbackParameters)
            throws ApiException {
        com.squareup.okhttp.Call call =
                policyDeploymentRollbackPutValidateBeforeCall(policyRollbackParameters, null, null);
        return apiClient.execute(call);
    }

    /**
     * Rollback a policy on the PDP to an older version (asynchronously)
     * 
     * @param policyRollbackParameters The parameters defining the rollback (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body
     *         object
     */
    public com.squareup.okhttp.Call policyDeploymentRollbackPutAsync(PolicyRollbackParameters policyRollbackParameters,
            final ApiCallback<Void> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = callback::onDownloadProgress;
            progressRequestListener = callback::onUploadProgress;
        }

        com.squareup.okhttp.Call call = policyDeploymentRollbackPutValidateBeforeCall(policyRollbackParameters,
                progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }

    /**
     * Build call for policyDeploymentUpgradePut.
     * 
     * @param policyUpgradeParameters The parameters defining the upgrade (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call policyDeploymentUpgradePutCall(PolicyUpgradeParameters policyUpgradeParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Map<String, String> localVarHeaderParams = new HashMap<>();

        final String[] localVarAccepts = {

        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put(ACCEPT, localVarAccept);
        }

        final String[] localVarContentTypes = {APPLICATION_JSON};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put(CONTENT_TYPE, localVarContentType);

        if (progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain)
                        throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener)).build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(POLICY_DEPLOYMENT_UPGRADE, "PUT", new ArrayList<>(), new ArrayList<>(),
                policyUpgradeParameters, localVarHeaderParams, new HashMap<>(), localVarAuthNames,
                progressRequestListener);
    }

    private com.squareup.okhttp.Call policyDeploymentUpgradePutValidateBeforeCall(
            PolicyUpgradeParameters policyUpgradeParameters,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        return policyDeploymentUpgradePutCall(policyUpgradeParameters, progressListener, progressRequestListener);
    }

    /**
     * Upgrade a policy on the PDP to a newer version
     * 
     * @param policyUpgradeParameters The parameters defining the upgrade (optional)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public void policyDeploymentUpgradePut(PolicyUpgradeParameters policyUpgradeParameters) throws ApiException {
        policyDeploymentUpgradePutWithHttpInfo(policyUpgradeParameters);
    }

    /**
     * Upgrade a policy on the PDP to a newer version
     * 
     * @param policyUpgradeParameters The parameters defining the upgrade (optional)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the
     *         response body
     */
    public ApiResponse<Void> policyDeploymentUpgradePutWithHttpInfo(PolicyUpgradeParameters policyUpgradeParameters)
            throws ApiException {
        com.squareup.okhttp.Call call =
                policyDeploymentUpgradePutValidateBeforeCall(policyUpgradeParameters, null, null);
        return apiClient.execute(call);
    }

    /**
     * Upgrade a policy on the PDP to a newer version (asynchronously)
     * 
     * @param policyUpgradeParameters The parameters defining the upgrade (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body
     *         object
     */
    public com.squareup.okhttp.Call policyDeploymentUpgradePutAsync(PolicyUpgradeParameters policyUpgradeParameters,
            final ApiCallback<Void> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = callback::onDownloadProgress;
            progressRequestListener = callback::onUploadProgress;
        }

        com.squareup.okhttp.Call call = policyDeploymentUpgradePutValidateBeforeCall(policyUpgradeParameters,
                progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
}
