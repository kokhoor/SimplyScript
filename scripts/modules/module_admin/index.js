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
  getPermissions(args, ctx) { // get list of permissions by a specific module, or null for all
    var user = ctx.getUser();
    if (user == null || !user.is_superuser)
        raiseError("Not Authorized", "E_NOTAUTHORIZED", ctx.getLoggerName());

    var config = ctx.system("config");
    var module = config["module"];
    var allowed = module.allow;
    if (module.allowed == null)
      allowed = {};
    var denied = module.deny;
    if (module.denied == null)
      denied = {};
    var map = module.map;
    if (module.map == null) {
      module.map = {};
    }
    
    // For mapped module, the value (the map destination) if not in allowed and does not exist as key in map, then do not process it
    for (var i in module.map) {
      var m = module.map[i];
      if (m in module.map) // the value exists as a key in map
        continue;
      if (m in allowed) // exists in allowed
        continue;

      denied[m] = true;
    }
    var scripts_path = module['path'];
    var java_io_Files = Java.type("java.io.File");
    var FileFilter = Java.type("java.io.FileFilter");
    var directoryFilter = new FileFilter() {
			accept: function(file) {
				return file.isDirectory();
			}
		};
    var files = new java_io_Files(scripts_path).listFiles(directoryFilter);
    if (files != null) {
      for (var i=0; i<files.length; i++) {
        var module_name = files[i].getName();
        if (module_name in denied) {
          console.log("skip module: " + module_name);
          continue;
        }
        this._processModule(module_name, ctx);
      }
      for (var module_name in module.map) {
        if (module_name in denied) {
          console.log("skip module: " + module_name);
          continue;
        }
        this._processModule(module_name, ctx);
      }
    }
  },
  _processModule(module_name, ctx) {
    var objModule = ctx.module(module_name);
    if (objModule == null) {
      console.log("Module not found: " + module);
    }
    for (var i in objModule) {
      if (objModule[i] instanceof Function && !(i.startsWith('_'))) {
        console.log(`${module_name}.${i}`);
      }
    }
  }
});
