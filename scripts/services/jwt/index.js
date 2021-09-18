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

(function() {
  
function jwt() {
}

jwt.prototype = {
  _init() {
  },
  _setup(serviceName, args, system, path, ctx) {
    this._loggername = "services." + serviceName;
    log.info(this, "Service Name: {}, my path is: {}, args: {}", serviceName, path, args);
    ctx.localContext.addClasspath(path + 'dependency/');
    ctx.localContext.addClasspath(path + 'dependency/*.jar');

    return {
      contextPrototype: this,
      preCall: {fn: this.preCall, priority: 9000}
    };
  },
  getLoggerName() {
    return this._loggername;
  },
  preCall(ctx, e, action, args) {
    
  }
};

var service = new jwt();
service._init();
return service;

}());