# papPdpClient

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
    <groupId>org.onap.policy</groupId>
    <artifactId>papPdpClient</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "org.onap.policy:papPdpClient:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/papPdpClient-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import org.onap.policy.pap.*;
import org.onap.policy.pap.auth.*;
import org.onap.policy.pap.model.*;
import org.onap.policy.pap.DefaultApi;

import java.io.File;
import java.util.*;

public class DefaultApiExample {

    public static void main(String[] args) {
        
        DefaultApi apiInstance = new DefaultApi();
        PolicyActivationParameters policyActivationParameters = new PolicyActivationParameters(); // PolicyActivationParameters | The parameters defining the activation
        try {
            apiInstance.policyDeploymentActivatePut(policyActivationParameters);
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#policyDeploymentActivatePut");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *https://localhost*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*DefaultApi* | [**policyDeploymentActivatePut**](docs/DefaultApi.md#policyDeploymentActivatePut) | **PUT** /policyDeployment/activate | Activate a policy on the PDP
*DefaultApi* | [**policyDeploymentDeployPut**](docs/DefaultApi.md#policyDeploymentDeployPut) | **PUT** /policyDeployment/deploy | Deploys a policy to the PDP
*DefaultApi* | [**policyDeploymentModePut**](docs/DefaultApi.md#policyDeploymentModePut) | **PUT** /policyDeployment/mode | Set the mode of the PDP
*DefaultApi* | [**policyDeploymentRetirePut**](docs/DefaultApi.md#policyDeploymentRetirePut) | **PUT** /policyDeployment/retire | Retire a policy from the PDP
*DefaultApi* | [**policyDeploymentRollbackPut**](docs/DefaultApi.md#policyDeploymentRollbackPut) | **PUT** /policyDeployment/rollback | Rollback a policy on the PDP to an older version
*DefaultApi* | [**policyDeploymentUpgradePut**](docs/DefaultApi.md#policyDeploymentUpgradePut) | **PUT** /policyDeployment/upgrade | Upgrade a policy on the PDP to a newer version


## Documentation for Models

 - [ModeParameters](docs/ModeParameters.md)
 - [Policy](docs/Policy.md)
 - [PolicyActivationParameters](docs/PolicyActivationParameters.md)
 - [PolicyFile](docs/PolicyFile.md)
 - [PolicyIdentity](docs/PolicyIdentity.md)
 - [PolicyRetirementParameters](docs/PolicyRetirementParameters.md)
 - [PolicyRollbackParameters](docs/PolicyRollbackParameters.md)
 - [PolicyUpgradeParameters](docs/PolicyUpgradeParameters.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author



