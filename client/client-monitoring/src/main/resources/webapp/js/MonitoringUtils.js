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
 * Crate a dialog with input, attach it to a given parent and show an optional message
 */
function papDialogForm_activate(formParent, message) {
    papUtils_removeElement("papDialogDiv");

    var contentelement = document.createElement("papDialogDiv");
    var formDiv = document.createElement("div");
    var backgroundDiv = document.createElement("div");
    backgroundDiv.setAttribute("id", "papDialogDivBackground");
    backgroundDiv.setAttribute("class", "papDialogDivBackground");

    backgroundDiv.appendChild(formDiv);
    contentelement.appendChild(backgroundDiv);
    formParent.appendChild(contentelement);

    formDiv.setAttribute("id", "papDialogDiv");
    formDiv.setAttribute("class", "papDialogDiv");

    var headingSpan = document.createElement("span");
    formDiv.appendChild(headingSpan);

    headingSpan.setAttribute("class", "headingSpan");
    headingSpan.innerHTML = "PAP Configuration";

    var form = document.createElement("papDialog");
    formDiv.appendChild(form);

    form.setAttribute("id", "papDialog");
    form.setAttribute("class", "form-style-1");
    form.setAttribute("method", "post");

    if (message) {
        var messageLI = document.createElement("li");
        messageLI.setAttribute("class", "dialogMessage");
        messageLI.innerHTML = message;
        form.appendChild(messageLI);
    }

    var services = localStorage.getItem("pap-monitor-services_old");
    //url
    var input = createDialogList(form, "papDialogUrlInput","Pap rest baseURL:", "services_url_input", "http://localhost:12345", (services && services !== "null") ? JSON.parse(services).useHttps + "://" + JSON.parse(services).hostname + ":"
    + JSON.parse(services).port : "");

    //UserName
    createDialogList(form, "papDialogUsernameInput","Pap UserName:", "services_username_input", "username", (services && services !== "null") ? JSON.parse(services).username : "");
 
    //Password
    createDialogList(form, "papDialogPasswordInput","Pap Password:", "services_password_input", "password", (services && services !== "null") ? JSON.parse(services).password : "");

    //submit
    var inputLI = document.createElement("li");
    form.appendChild(inputLI);
    var submitInput = document.createElement("input");
    submitInput.setAttribute("id", "submit");
    submitInput.setAttribute("class", "button ebBtn");
    submitInput.setAttribute("type", "submit");
    submitInput.setAttribute("value", "Submit");
    submitInput.onclick = papDialogForm_submitPressed;
    inputLI.appendChild(submitInput);

    // Enter key press triggers submit
    $(input).keyup(function(event) {
        if (event.keyCode == 13) {
            $(submitInput).click();
        }
    });

    input.focus();
}

function createDialogList(form, forA, reminder, id, placeholder, value_old){
    var diaLI = document.createElement("li");
    form.appendChild(diaLI);

    var diaLabel = document.createElement("label");
    diaLI.appendChild(diaLabel);

    diaLabel.setAttribute("for", forA);
    diaLabel.innerHTML = reminder;

    var diaLabelSpan = document.createElement("span");
    diaLabel.appendChild(diaLabelSpan);

    diaLabelSpan.setAttribute("class", "required");
    diaLabelSpan.innerHTML = "*";

    var input = document.createElement("input");
    input.setAttribute("id", id);
    input.setAttribute("placeholder", placeholder);
    input.value = value_old;
    diaLI.appendChild(input);
    return input;
}

/*
 * Create a dialog for displaying text
 */
function papTextDialog_activate(formParent, message, title) {
    papUtils_removeElement("papDialogDiv");

    var contentelement = document.createElement("div");
    contentelement.setAttribute("id", "papDialogDiv")
    var formDiv = document.createElement("div");
    var backgroundDiv = document.createElement("div");
    backgroundDiv.setAttribute("id", "papDialogDivBackground");
    backgroundDiv.setAttribute("class", "papDialogDivBackground");

    backgroundDiv.appendChild(formDiv);
    contentelement.appendChild(backgroundDiv);
    formParent.appendChild(contentelement);

    formDiv.setAttribute("id", "papErrorDialogDiv");
    formDiv.setAttribute("class", "papDialogDiv papErrorDialogDiv");

    var headingSpan = document.createElement("span");
    formDiv.appendChild(headingSpan);

    headingSpan.setAttribute("class", "headingSpan");
    headingSpan.innerHTML = title;

    var form = document.createElement("div");
    formDiv.appendChild(form);

    form.setAttribute("id", "papDialog");
    form.setAttribute("class", "form-style-1");
    form.setAttribute("method", "post");

    if (message) {
        var messageLI = document.createElement("li");
        messageLI.setAttribute("class", "dialogMessage");
        messageLI.innerHTML = message;
        form.appendChild(messageLI);
    }

    var inputLI = document.createElement("li");
    form.appendChild(inputLI);

    var cancelInput = document.createElement("input");
    cancelInput.setAttribute("class", "button ebBtn");
    cancelInput.setAttribute("type", "submit");
    cancelInput.setAttribute("value", "Close");
    cancelInput.onclick = newModelForm_cancelPressed;
    form.appendChild(cancelInput);
}

/*
 * Create a Success dialog
 */
function papSuccessDialog_activate(formParent, message) {
    papTextDialog_activate(formParent, message, "Success");
}

/*
 * Create an Error dialog
 */
function papErrorDialog_activate(formParent, message) {
    papTextDialog_activate(formParent, message, "Error");
}

/*
 * Dialog cancel callback
 */
function newModelForm_cancelPressed() {
    papUtils_removeElement("papDialogDivBackground");
}

/*
 * Dialog submit callback
 */
function papDialogForm_submitPressed() {
    var url = $('#services_url_input').val();
    var userName = $('#services_username_input').val();
    var passWord = $('#services_password_input').val();
    if (url && url.length > 0 && userName && userName.length > 0 && passWord && passWord.length > 0) {
        var engineConfig = {
            useHttps : url.split(":")[0] == "https"? "https": "http",
            hostname : url.split(":")[1].split("//")[1],
            port : url.split(":")[2],
            username : userName,
            password : passWord
        };
        localStorage.setItem("pap-monitor-services_old", JSON.stringify(engineConfig));
        localStorage.setItem("pap-monitor-services", JSON.stringify(engineConfig));
        papUtils_removeElement("papDialogDivBackground");
        getEngineURL();
    }
}

/*
 * Remove an element from the page
 */
function papUtils_removeElement(elementname) {
    var element = document.getElementById(elementname);
    if (element != null) {
        element.parentNode.removeChild(element);
    }
}

/*
 * Compare two objects
 */
function deepCompare() {
    var i, l, leftChain, rightChain;

    function compare2Objects(x, y) {
        var p;

        // remember that NaN === NaN returns false
        // and isNaN(undefined) returns true
        if (isNaN(x) && isNaN(y) && typeof x === 'number' && typeof y === 'number') {
            return true;
        }

        // Compare primitives and functions.
        // Check if both arguments link to the same object.
        // Especially useful on the step where we compare prototypes
        if (x === y) {
            return true;
        }

        // Works in case when functions are created in constructor.
        // Comparing dates is a common scenario. Another built-ins?
        // We can even handle functions passed across iframes
        if ((typeof x === 'function' && typeof y === 'function') || (x instanceof Date && y instanceof Date)
                || (x instanceof RegExp && y instanceof RegExp) || (x instanceof String && y instanceof String)
                || (x instanceof Number && y instanceof Number)) {
            return x.toString() === y.toString();
        }

        // At last checking prototypes as good as we can
        if (!(x instanceof Object && y instanceof Object)) {
            return false;
        }

        if (x.isPrototypeOf(y) || y.isPrototypeOf(x)) {
            return false;
        }

        if (x.constructor !== y.constructor) {
            return false;
        }

        if (x.prototype !== y.prototype) {
            return false;
        }

        // Check for infinitive linking loops
        if (leftChain.indexOf(x) > -1 || rightChain.indexOf(y) > -1) {
            return false;
        }

        // Quick checking of one object being a subset of another.
        // todo: cache the structure of arguments[0] for performance
        for (p in y) {
            if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
                return false;
            } else if (typeof y[p] !== typeof x[p]) {
                return false;
            }
        }

        for (p in x) {
            if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
                return false;
            } else if (typeof y[p] !== typeof x[p]) {
                return false;
            }

            switch (typeof (x[p])) {
            case 'object':
            case 'function':

                leftChain.push(x);
                rightChain.push(y);

                if (!compare2Objects(x[p], y[p])) {
                    return false;
                }

                leftChain.pop();
                rightChain.pop();
                break;

            default:
                if (x[p] !== y[p]) {
                    return false;
                }
                break;
            }
        }

        return true;
    }

    if (arguments.length < 1) {
        return true;
    }

    for (i = 1, l = arguments.length; i < l; i++) {

        leftChain = []; // Todo: this can be cached
        rightChain = [];

        if (!compare2Objects(arguments[0], arguments[i])) {
            return false;
        }
    }

    return true;
}

function getHomepageURL() {
    var homepageURL = location.protocol
            + "//"
            + window.location.hostname
            + (location.port ? ':' + location.port : '')
            + (location.pathname.endsWith("/monitoring/") ? location.pathname.substring(0, location.pathname
                    .indexOf("monitoring/")) : location.pathname);
    location.href = homepageURL;
}

function removeChildrenElementsByClass(className){
    var elements = document.getElementsByClassName(className);
    elements[0].innerHTML = '';
}