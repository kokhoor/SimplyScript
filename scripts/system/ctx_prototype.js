/* global Java */

load(scripts_path + 'system/priority_list.js');
(function() {

function ctxObject(argLocalContext) {
  localContext = argLocalContext;
  this.callDepth = -1;
  this.callStack = [];
  user = {
    "username": "anonymous",
    "is_active": false,
    "is_staff": false,
    "is_superuser": false,
    "is_anonymous": true
  };
}

ctxObject.prototype = {
  getLoggerName() {
    if (this.callStack.length == 0)
      return "context";
    return "modules." + this.callStack[this.callStack.length-1];
  },
  getUser() {
    return user;
  },
  setUser(argUser, uniqueid) {
    if (localContext.isPrivileged(uniqueid))
      user = argUser;
    else
      raiseError("Caller does not have privilege to set context User", "E_NOPRIVILEGE_SETUSER");
  },
  system(name) {
    var user = this.getUser();
    if (user == null || !user.is_superuser)
        raiseError("Not Authorized", "E_NOTAUTHORIZED", this.getLoggerName());
    return localContext.system(name);
  },
  module(name) {
    var user = this.getUser();
    if (user == null || !user.is_superuser)
        raiseError("Not Authorized", "E_NOTAUTHORIZED", this.getLoggerName());
    console.log("Trying to load module: " + name);
    return localContext.module(name, this);
  },
  addClasspath(path) {
    localContext.addClasspath(path);
  },
  app(key, value) {
    if (arguments.length > 1) { // is set
      localContext.app(key, value);
    } else { // is get
      return  localContext.app(key);
    }
  },
  cache(key, value) {
    if (arguments.length > 1) { // is set
      localContext.cache(key, value);
    } else { // is get
      return  localContext.cache(key);
    }
  },
  req(key, value) {
    if (arguments.length > 1) { // is set
      localContext.req(key, value);
    } else { // is get
      return  localContext.req(key);
    }
  },
  addReturnCommand(value) {
    var array_request_commands = Java.type("my.com.solutionx.simplyscript.ScriptEngineInterface").PROCESS_COMMANDS;
    var arr = this.req(array_request_commands);
    if (arr == null) {
      arr = [];
      this.req(array_request_commands, arr);
    }
    arr.push(value);
  },
  setReturn(key, value) {
    var map_return_key = Java.type("my.com.solutionx.simplyscript.ScriptEngineInterface").OTHER_RETURN_DATA;
    var map = this.req(map_return_key);
    if (map == null) {
      map = {};
      this.req(map_return_key, map);
    }
    map[key] = value;
  },
  call(action, args) {
    var idx = action.lastIndexOf(".");
    if (idx === -1)
      throw new Error("Invalid action format. Expected XXX.YYY");

    this.callDepth += 1;
    this.callStack.push(action);
    try {
      var preCall = this.callDepth <= 0 ? localContext.system("preCall") : localContext.system("preInnerCall");
      if (preCall !== null) {
        for (var i=0; i<preCall.items.length; i++) {
          try {
            preCall.items[i].fn.call(preCall.items[i]['this'], this, null, action, args);
          } catch (e) {
            console.log("preCall error: " + e);
          }
        }
      }

      var module = action.substring(0, idx);
      var method = action.substring(idx+1);

      var objModule = localContext.module(module, this);
      if (objModule == null)
        throw new Error("Module not found: " + module);

      var ret = objModule[method](args, this);

      var postCall = this.callDepth <= 0 ? localContext.system("postCall") : localContext.system("postInnerCall");
      if (postCall !== null) {
        for (var i=0; i<postCall.items.length; i++) {
          try {
            postCall.items[i].fn.call(postCall.items[i]['this'], this, null, action, args);
          } catch (e) {
            console.log("postCall error: " + e);
          }
        }
      }

      return ret;
    } catch (e) {
      var postCall = this.callDepth <= 0 ? localContext.system("postCall") : localContext.system("postInnerCall");
      if (postCall !== null) {
        for (var i=0; i<postCall.items.length; i++) {
          try {
            postCall.items[i].fn.call(postCall.items[i]['this'], this, e, action, args);
          } catch (e) {
            console.log("postCall error: " + e);
          }
        }
      }
      throw e;
    } finally {
      this.callDepth -= 1;
      this.callStack.pop();
    }
  },
  service(name) {
    return localContext.service(name, this);
  }
/*
  module(name) {
    return localContext.module(module, this);
  }
*/
};

ctxObject.serviceSetup = function(serviceName, system, uniqueid, ctx) {
  if (this._config.service.deny != null) {
    if (serviceName in this._config.service.deny) {
      return null;
    }
  }

  if (this._config.service.allow != null) {
    if (!(serviceName in this._config.service.allow)) {
      return null;
    }
  }

  var scriptName = serviceName;
  if (this._config.service.map != null) {
    var mappedScript = this._config.service.map[serviceName];
    if (mappedScript != null)
      scriptName = mappedScript;
  }

  var setupData = null;
  var path = `${this._config.service.path}/${scriptName}/`;
  var serviceConstructor = load(path + 'index.js');
  if (serviceConstructor == null)
    return null;

  var service = new serviceConstructor(uniqueid);
  if (service == null)
    return null;

  if ("_init" in service)
    service._init();

  if ("_setup" in service) {
    var args = this._config.service.initArguments[serviceName] || {};
    setupData = service._setup(serviceName, args, system, path, ctx);
  }
  if (setupData == null)
    return service;

  if (setupData.contextPrototype != null) {
    ctxObject.prototype[serviceName] = setupData.contextPrototype;
  }

  var keys = ["preCall", "postCall", "preInnerCall", "postInnerCall"];
  for (var i=0; i<keys.length; i++) {
    var call = setupData[keys[i]];
    if (call != null) {
      var call_array = system[keys[i]];
      if (call_array == null) {
        // if post create ascending list (call low priority to high priority, otherwise descending
        call_array = system[keys[i]] = new PriorityList(keys[i].startsWith("post"));
      }
      if (!("this" in call)) {
        call['this'] = service;
      }
      call_array.add(call);
    }
  }
  return service;
};

ctxObject.moduleSetup = function (moduleName, system, ctx) {
  if (this._config.module.deny != null) {
    if (moduleName in this._config.module.deny) {
      return null;
    }
  }

  if (this._config.module.allow != null) {
    if (!(moduleName in this._config.module.allow)) {
      return null;
    }
  }

  var scriptName = moduleName;
  if (this._config.module.map != null) {
    var mappedScript = this._config.module.map[moduleName];
    if (mappedScript != null)
      scriptName = mappedScript;
  }
  var path = `${this._config.module.path}/${scriptName}/`;
  var module = load(path + 'index.js');
  if ("_setup" in module) {
    var args = this._config.service.initArguments[moduleName] || {};
    setupData = module._setup(moduleName, args, system, path, ctx);
  }
  return module;
};

function array_to_map(obj) {
  var map = {};
  for (var i=0; i<obj.length; i++) {
    map[obj[i]] = true;
  }
  return map;
}

ctxObject.config = function (objs) {
  function config_by_type(type, obj) {
    if (obj.allow === "*")
      obj.allow = null;

    if (obj.allow != null) {
      obj.allow = array_to_map(obj.allow);
    }

    if (obj.deny != null) {
      obj.deny = array_to_map(obj.deny);
    }

    if (obj.initArguments == null) {
      obj.initArguments = {};
    }
    ctxObject._config[type] = obj;
  }

  if (!ctxObject._config) {
    ctxObject._config = {
      service: {},
      module: {}
    };
  }

  config_by_type("service", objs['service']);
  config_by_type("module", objs['module']);
};

function newContext(localContext) {
  var o = new ctxObject(localContext);
  return Object.freeze(o);
}
return [ctxObject, newContext];

});