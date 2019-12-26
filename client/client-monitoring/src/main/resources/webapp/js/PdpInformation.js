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

/*
 * Create the Engine Service Table 
 */
function createEngineServiceTable() {
    var tableId = config.engineService.tableId;
    var headers = config.engineService.headers;
    var table = createEngineTable($("." + config.engineService.parent), tableId, headers.map(function(a) {
        return a.title;
    }));
    var tableRow = document.createElement("tr");
    var tableData = "";
    for ( var h in headers) {
        tableData += "<td id=" + tableId + "_" + headers[h].id + "></td>";
    }
    tableRow.innerHTML = tableData;
    $(table).children("#engineTableBody").append(tableRow);
}

/*
 * Check for any changes in the Engine Service Table data and update only where
 * necessary
 */
function setEngineServiceData(engineId, groupName, subGroupName, server, port) {
    this.engineId = engineId;
    var tableId = config.engineService.tableId;
    var headers = config.engineService.headers.map(function(a) {
        return a.id;
    });
    var data = [ engineId, groupName, subGroupName, server + ":" + port ];

    var engineServiceTable = $("#engineServicesTable");

    for ( var h in headers) {
        var td = engineServiceTable.find("#" + tableId + "_" + headers[h]);
        if (td.html() !== data[h]) {
            engineServiceTable.find("#" + tableId + "_" + headers[h]).html(data[h]);
        }
    }
}
