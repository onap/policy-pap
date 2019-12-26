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

function RenderPdpList(treeArr, className) {
    var $ = treeArr,
        root = document.createDocumentFragment(),
        childLevel = 0
        var index=''
        var isNode=false
    function insertChildren(parentNode, traverseArr, subGroup) {

        for(let i = 0; i < traverseArr.length; i++) {
            if(parentNode === root) {
                childLevel = 0
            }
            var currentLi = document.createElement('li')
            currentLi.setAttribute('level', childLevel)
            if(traverseArr[i].children && traverseArr[i].children.length > 0) {
                var title = document.createElement('div')
                var triangle = document.createElement('i')
                var text = document.createElement('p')
                currentLi.classList.add('parentNode')
                title.classList.add('title')
                triangle.classList.add('triangle')
                text.innerText = traverseArr[i].title
                title.appendChild(triangle)
                title.appendChild(text)
                currentLi.appendChild(title)
                childLevel++
                if(isNode) index=""
                if(subGroup !== null){
                    index+= subGroup+"/"
                }
                insertChildren(currentLi, traverseArr[i].children, traverseArr[i].title)
            }else {
                var a = document.createElement('a')
                a.setAttribute('href',"#"+index+subGroup+"/"+traverseArr[i].title)
                a.classList.add('pdps__link')
                a.textContent= traverseArr[i].title
                currentLi.appendChild(a)
                isNode=true
            }
            parentNode.appendChild(currentLi)
        }
    }
    insertChildren(root, $, null)
    document.querySelector('ul.' + className + '').appendChild(root)
}

function highlightSelected (id){
    const resultsArr = Array.from(document.querySelectorAll('.pdps__link'));
    resultsArr.forEach(el => {
        el.classList.remove('pdps__link--active');
    });
    document.querySelector(`.pdps__link[href*="${id}"]`).classList.add('pdps__link--active');
};