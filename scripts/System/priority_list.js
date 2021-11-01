/* 
 * Copyright 2021 SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;.
 *
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
 */
// Priority runs from 0 (lowest) to 10000 (highest). Default is 5000
function PriorityList(ascending) {
  this.items = [];
  this.ascending = ascending; // if true means smallest to biggest
}

PriorityList.prototype.add = function (item) {
  if (this.items.length === 0) {
    this.items.push(item);
    return;
  }
  var contain = false;
  if (item.priority == null)
    item.priority = 5000;
  if (this.ascending) {
    for (var i = 0; i < this.items.length; i++) {
      if (this.items[i].priority >= item.priority) {
        this.items.splice(i, 0, item);
        contain = true;
        break;
      }
    }
    if (!contain) {
        this.items.push(item);
    }
  } else {
    for (var i=this.items.length-1; i>=0; i--) {
      if (this.items[i].priority >= item.priority) {
        this.items.splice(i+1, 0, item);
        contain = true;
        break;
      }
    }
    if (!contain) {
        this.items.unshift(item);
    }
  }
};

/*** TEST CASES
var asc = new PriorityList(true); // build an asending priority queue (low to high priority)
asc.add({name:"a", priority:1000});
asc.add({name:"c", priority:10000});
asc.add({name:"b", priority:3000});
asc.add({name:"b2"});
asc.add({name:"c2", priority:20000});
asc.add({name:"c2.2", priority:20000});
asc.add({name:"a2", priority:0});
asc.add({name:"a12", priority:1000});
console.log(asc.items);

var desc = new PriorityList(false); // build a descending priority queue (high to low priority)
desc.add({name:"a", priority:1000});
desc.add({name:"c", priority:10000});
desc.add({name:"b", priority:3000});
desc.add({name:"b2"});
desc.add({name:"c2", priority:20000});
desc.add({name:"a2", priority:0});
desc.add({name:"a12", priority:1000});
console.log(desc.items);
***/

