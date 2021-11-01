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

({
  _setup(moduleName, args, system, path, ctx) {
    this._loggername = "modules." + moduleName;
    log.info(this, "Module name: {} my path is: {}", moduleName, path);
  },
  getLoggerName() {
    return this._loggername;
  },
  reload(args, ctx) {
    var user = ctx.getUser();
    if (user == null || !user.is_superuser)
        raiseError("Not Authorized", "E_NOTAUTHORIZED", ctx.getLoggerName());
    ctx.addReturnCommand("reload");
  }
});
