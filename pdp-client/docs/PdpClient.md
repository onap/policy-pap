# DefaultApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**policyDeploymentActivatePut**](DefaultApi.md#policyDeploymentActivatePut) | **PUT** /policyDeployment/activate | Activate a policy on the PDP
[**policyDeploymentDeployPut**](DefaultApi.md#policyDeploymentDeployPut) | **PUT** /policyDeployment/deploy | Deploys a policy to the PDP
[**policyDeploymentModePut**](DefaultApi.md#policyDeploymentModePut) | **PUT** /policyDeployment/mode | Set the mode of the PDP
[**policyDeploymentRetirePut**](DefaultApi.md#policyDeploymentRetirePut) | **PUT** /policyDeployment/retire | Retire a policy from the PDP
[**policyDeploymentRollbackPut**](DefaultApi.md#policyDeploymentRollbackPut) | **PUT** /policyDeployment/rollback | Rollback a policy on the PDP to an older version
[**policyDeploymentUpgradePut**](DefaultApi.md#policyDeploymentUpgradePut) | **PUT** /policyDeployment/upgrade | Upgrade a policy on the PDP to a newer version


<a name="policyDeploymentActivatePut"></a>
# **policyDeploymentActivatePut**
> policyDeploymentActivatePut(policyActivationParameters)

Activate a policy on the PDP

### Example
```java
// Import classes:
//import org.onap.policy.pap.ApiException;
//import org.onap.policy.pap.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
PolicyActivationParameters policyActivationParameters = new PolicyActivationParameters(); // PolicyActivationParameters | The parameters defining the activation
try {
    apiInstance.policyDeploymentActivatePut(policyActivationParameters);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#policyDeploymentActivatePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyActivationParameters** | [**PolicyActivationParameters**](PolicyActivationParameters.md)| The parameters defining the activation | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a name="policyDeploymentDeployPut"></a>
# **policyDeploymentDeployPut**
> policyDeploymentDeployPut(policyDeploymentParameters)

Deploys a policy to the PDP

### Example
```java
// Import classes:
//import org.onap.policy.pap.ApiException;
//import org.onap.policy.pap.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
Policy policyDeploymentParameters = new Policy(); // Policy | The parameters defining the deployment
try {
    apiInstance.policyDeploymentDeployPut(policyDeploymentParameters);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#policyDeploymentDeployPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyDeploymentParameters** | [**Policy**](Policy.md)| The parameters defining the deployment | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a name="policyDeploymentModePut"></a>
# **policyDeploymentModePut**
> policyDeploymentModePut(modeParameters)

Set the mode of the PDP

### Example
```java
// Import classes:
//import org.onap.policy.pap.ApiException;
//import org.onap.policy.pap.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
ModeParameters modeParameters = new ModeParameters(); // ModeParameters | The parameters defining the mode
try {
    apiInstance.policyDeploymentModePut(modeParameters);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#policyDeploymentModePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modeParameters** | [**ModeParameters**](ModeParameters.md)| The parameters defining the mode | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a name="policyDeploymentRetirePut"></a>
# **policyDeploymentRetirePut**
> policyDeploymentRetirePut(policyRetirementParameters)

Retire a policy from the PDP

### Example
```java
// Import classes:
//import org.onap.policy.pap.ApiException;
//import org.onap.policy.pap.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
PolicyRetirementParameters policyRetirementParameters = new PolicyRetirementParameters(); // PolicyRetirementParameters | The parameters defining the retirement
try {
    apiInstance.policyDeploymentRetirePut(policyRetirementParameters);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#policyDeploymentRetirePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyRetirementParameters** | [**PolicyRetirementParameters**](PolicyRetirementParameters.md)| The parameters defining the retirement | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a name="policyDeploymentRollbackPut"></a>
# **policyDeploymentRollbackPut**
> policyDeploymentRollbackPut(policyRollbackParameters)

Rollback a policy on the PDP to an older version

### Example
```java
// Import classes:
//import org.onap.policy.pap.ApiException;
//import org.onap.policy.pap.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
PolicyRollbackParameters policyRollbackParameters = new PolicyRollbackParameters(); // PolicyRollbackParameters | The parameters defining the rollback
try {
    apiInstance.policyDeploymentRollbackPut(policyRollbackParameters);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#policyDeploymentRollbackPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyRollbackParameters** | [**PolicyRollbackParameters**](PolicyRollbackParameters.md)| The parameters defining the rollback | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a name="policyDeploymentUpgradePut"></a>
# **policyDeploymentUpgradePut**
> policyDeploymentUpgradePut(policyUpgradeParameters)

Upgrade a policy on the PDP to a newer version

### Example
```java
// Import classes:
//import org.onap.policy.pap.ApiException;
//import org.onap.policy.pap.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
PolicyUpgradeParameters policyUpgradeParameters = new PolicyUpgradeParameters(); // PolicyUpgradeParameters | The parameters defining the upgrade
try {
    apiInstance.policyDeploymentUpgradePut(policyUpgradeParameters);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#policyDeploymentUpgradePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **policyUpgradeParameters** | [**PolicyUpgradeParameters**](PolicyUpgradeParameters.md)| The parameters defining the upgrade | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

