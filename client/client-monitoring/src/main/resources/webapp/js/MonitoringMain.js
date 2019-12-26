/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

var restRootURL;

// Configuration used for page layout and charts
var config = {
    refresh : 5000,
    engineService : {
        parent : "engineService",
        tableId : "engineServicesTable",
        headers : [ {
            title : "Engine Service ID",
            id : "engine_id"
        }, {
            title : "GroupName",
            id : "group_name"
        }, {
            title : "SubGroupName",
            id : "subgroup_name"
        },{
            title : "server:port",
            id : "server_port"
        }]
    },
    engineSummary : {
        parent : "engineSummary",
        tableId : "engineSummaryTable",
        headers : [ {
            title : "Timestamp",
            id : "timestamp"
        }, {
            title : "Up Time(ms)",
            id : "up_time"
        }, {
            title : "Sum of policy deploy",
            id : "policy_deploy"
        }, {
            title : "Sum of policy deploy success",
            id : "policy_deploy_success"
        }, {
            title : "Sum of policy deploy fail",
            id : "policy_deploy_fail"
        }, {
            title : "Sum of policy executions",
            id : "policy_executions"
        }, {
            title : "Sum of policy executions success",
            id : "policy_executions_success"
        } , {
            title : "Sum of policy executions fail",
            id : "policy_executions_fail"
        }],
        chart : {
            avgPolicyDurationChart : {
                parent : "avgPolicyDuration",
                title : "Average Policy Duration (ms)",
                unit : "ms",
                lineStroke : "#5FBADD",
                nodeColour : "#00A9D4"
            },
        }
    },
    engineStatus : {
        parent : "engineStatus",
        tableId : "engineStatusTable",
        headers : [ {
            title : "Timestamp",
            id : "timestamp"
        }, {
            title : "Engine ID",
            id : "engine_id"
        }, {
            title : "Engine Status",
            id : "engine_status"
        }, {
            title : "Last Message",
            id : "last_message"
        }, {
            title : "Up Time (ms)",
            id : "up_time"
        }, {
            title : "Policy Executions",
            id : "policy_executions"
        } ]
    },
    engineChart : {
        lastPolicyDurationChart : {
            parent : "lastPolicyDurationChart",
            title : "Last Policy Duration (ms)",
            unit : "ms",
            lineStroke : "#F5A241",
            nodeColour : "#F08A00"
        },
        averagePolicyDurationChart : {
            parent : "averagePolicyDurationChart",
            title : "Average Policy Duration (ms)",
            unit : "ms",
            lineStroke : "#00625F",
            nodeColour : "#007B78"
        }
    }
};

function getPdpList(data) {
   const pdpArray = [];    
    for(let i = 0; i < data.groups.length; i++) {
        var map = {};
        map.title = data.groups[i].name;
        map.children = [];
        (data.groups[i].pdpSubgroups).forEach( (pdpSubgroup, index)=> {
            map.children[index] = {};
            map.children[index].title= pdpSubgroup.pdpType;
            const instanceId = [];
            pdpSubgroup.pdpInstances.forEach(pdpInstance => {
                var instanceIdMap = {};
                instanceIdMap.title = pdpInstance.instanceId; 
                instanceId.push(instanceIdMap)
                });
            map.children[index].children = instanceId;
        });
        pdpArray.push(map);
    }
    RenderPdpList(pdpArray, 'pdps__list');
}



function servicesCallback(data){
    var engineURL = localStorage.getItem("pap-monitor-services");
    if (engineURL) {
        this.engineURL = JSON.parse(engineURL);
        if (this.engineURL.hostname === data.server && this.engineURL.port === data.port) {
            setEngineServiceData(data.engine_id, this.groupName, this.subGroupName, data.server, data.port);
            setEngineSummaryData(data.status, data.timeStamp, data.policyDeployCount, data.policyDeploySuccessCount, data.policyDeployFailCount, data.policyExecutedCount, data.policyExecutedSuccessCount, data.policyExecutedFailCount);
            if(data.status.length){
                setEngineStatusData(data.status, this.changed);
                this.changed = false;
            }else{
                this.changed = true;
                removeChildrenElementsByClass(config.engineStatus.parent);
            }            
    
            // Make content visible after data has been returned for the first time
            if (!$(".content").is(':visible')) {
                $(".content").fadeIn();
            }
    
            // Repeat the same request
            setTimeout(function() {
                this.servicesCall = ajax_get_statistics(restRootURL+"statistics/", servicesCallback, this.engineURL.hostname, this.engineURL.port, this.id);
            }, config.refresh);
        }
    }    
}

/*
 * Clears and resets all content on the page
 */
function setUpPage() {
    // Hide all content
    $('#content').hide();

    // Clear each div
    $('#content > div').each(function() {
        $(this).empty();
    });

    // clear hashchange
    history.replaceState(null, null, ' ');

    //remove last search result of pdps.
    removeChildrenElementsByClass('pdps__list');

    // Reset trackers for tables
    this.engineStatusTables = [];
    this.engineContextTables = [];

    // Set up content div's
    createEngineServiceTable();
    createEngineSummaryTable();
}

/*
 * Retrieves the engine URL from the cookie. If it has not been set yet, then a
 * dialog is shown asking for it
 */
function getEngineURL(message) {
    // The engine URL is stored in a cookie using the key
    // "pap-monitor-services"
    var engineURL = localStorage.getItem("pap-monitor-services");

    // If an engine URL is stored in the cookie
    if (engineURL) {
        // Parse the engine URL
        this.engineURL = JSON.parse(engineURL);

        // Send a request with that engine URL
        ajax_get(restRootURL, getPdpList, this.engineURL.hostname, this.engineURL.port);
    } else {
        // Prompt for engine URL
        papDialogForm_activate(document.body, message);
    }
}

/*
 * Clears the cookie and reset the page
 */
function clearEngineURL() {

    if(typeof this.servicesCall !== "undefined"){
        this.servicesCall.abort();
    }

    // Remove engine URL from cookie
    localStorage.removeItem("pap-monitor-services");

    // Reset the page
    setUpPage();
}

/*
 * Called after the DOM is ready
 */
$(document).ready(
    function() {
        restRootURL = location.protocol
        + "//"
        + window.location.hostname
        + (location.port ? ':' + location.port : '')
        + (location.pathname.endsWith("/monitoring/") ? location.pathname.substring(0, location.pathname.indexOf("monitoring/")) : location.pathname) 
        + "papservices/monitoring/";
        // Initialize tooltip for the charts
        initTooltip();

        // Set up the structure of the page
        setUpPage();

        // Check cookies for engine URL
        getEngineURL();

        // Add click event to config icon for clearing engine URL
        $(".ebSystemBar-config").click(
            function() {
            // Clear the engine URL
            clearEngineURL();

            // Request the engine URL
            getEngineURL();
            }
        );

        ['hashchange', 'load'].forEach(event => window.addEventListener(event, function() {    
            // Get ID from url
            this.id = window.location.hash.replace('#', '');
            if(id !== ''){
                var arr = id.split("/");
                this.groupName = arr[0];
                this.subGroupName = arr[1];
                ajax_get_statistics(restRootURL+"statistics/", servicesCallback, this.engineURL.hostname, this.engineURL.port, id);
            }
        }));
    }
);