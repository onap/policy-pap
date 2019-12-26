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
 * Send a GET request
 */
function ajax_get(requestURL, callback, useHttps, hostname, port, username, password, params, errorCallback) {
    var data = {
        useHttps : useHttps,
        hostname : hostname,
        port : port,
        username : username,
        password : password
    };
    for ( var p in params) {
        data[p] = params[p];
    }
    return $.ajax({
        type : 'GET',
        url : requestURL,
        dataType : "json",
        data : data,
        success : function(data, textStatus, jqXHR) {
            if (callback) {
                callback(data);
            }
        },
        error : function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 500 || jqXHR.status == 404) {
                if (jqXHR.responseText.indexOf("cound not handshake with server") !== -1 || jqXHR.status == 404) {
                    clearEngineURL();
                    getEngineURL(jqXHR.responseText);
                } else {
                    papErrorDialog_activate(document.body, jqXHR.responseText);
                }
            }
            if (errorCallback) {
                errorCallback(jqXHR, textStatus, errorThrown);
            }
        }
    });
}

function ajax_get_statistics(requestURL, callback, useHttps, hostname, port, username, password, id, params, errorCallback) {
    var data = {
        useHttps : useHttps,
        hostname : hostname,
        port : port,
        username : username,
        password : password,
        id : id
    };
    for ( var p in params) {
        data[p] = params[p];
    }
    return $.ajax({
        type : 'GET',
        url : requestURL,
        dataType : "json",
        data : data,
        success : function(data, textStatus, jqXHR) {
            if (callback) {
                callback(data);
            }
        },
        error : function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 500 || jqXHR.status == 404) {
                if (jqXHR.responseText.indexOf("cound not handshake with server") !== -1 || jqXHR.status == 404) {
                    clearEngineURL();
                    getEngineURL(jqXHR.responseText);
                } else {
                    papErrorDialog_activate(document.body, jqXHR.responseText);
                }
            }
            if (errorCallback) {
                errorCallback(jqXHR, textStatus, errorThrown);
            }
        }
    });
}
