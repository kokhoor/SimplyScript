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

/* global log, Java */

(function() {
  
function requests() {
}

requests.prototype = {
  _init() {
  },
  _setup(serviceName, args, system, path, ctx) {
    ctx.addClasspath(path + 'dependency/');
    ctx.addClasspath(path + 'dependency/*.jar');
    this.requests = Java.type('my.com.solutionx.simplyscript_service.requests.Requests');

    return {
      contextPrototype: this
    };
  },
  getLoggerName() {
    return this._loggername;
  },
  call(method, args, ctx) {
    switch (method) {
      case "post":
      case "POST":
        return this.post(args, ctx);
      case "get":
      case "GET":
      default:
        return this.get(args, ctx);
    }
  },
  get(args, ctx) {
    return this.requests.get(args);
  },
  post(args, ctx) {
    return this.requests.post(args);
  },
};

return requests;

}());