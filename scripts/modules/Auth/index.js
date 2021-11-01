/* global Java */

({
  login(args, ctx) {
    check_empty(args, ['username', 'password'], ctx);
    var user = ctx.service("auth").verifyUserPassword(args.username, args.password, ctx);
    var groups = null;
    if (user != null) {
      var token = ctx.service("jwt").encode(user);
      ctx.setReturn("token", token);
      groups = ctx.db.selectList(null, "auth.getGroups", {
        username: user.username
      }, ctx);
    }
    return {
      "user": user,
      "groups": groups
    };
  },
  changePassword(args, ctx) {
    var user = ctx.getUser();
    if (user == null || user.is_anonymous)
      raiseError("Not logged in", "E_NOTLOGGEDIN", ctx.getLoggerName());

    check_empty(args, ['current', 'new', 'confirm'], ctx);

    if (args.current == args.new)
      raiseError("New password cannot be the same as Confirm password", "E_CHANGEPASSWORD_SAMECURRENT_NEW", ctx.getLoggerName());        
    if (args.new != args.confirm)
      raiseError("New password and Confirm password must match", "E_CHANGEPASSWORD_NOTMATCH_CURRENT_NEW", ctx.getLoggerName());        

    var verify_user = ctx.service("auth").verifyUserPassword(user.username, args.current, ctx);
    if (verify_user == null)
      raiseError("Current password is not valid", "E_CHANGEPASSWORD_INVALIDCURRENTPASSWORD", ctx.getLoggerName());        

    var ret = ctx.service("auth").setPassword(user.username, args.new, ctx);
    if (ret == 1)
      return;
    else if (ret > 1) {
      raiseError("Abnormal error. Change password will affect more than one user. Aborted", "E_CHANGEPASSWORD_MORETHANONERECORD", ctx.getLoggerName());
    } else
    raiseError("Failed to change password", "E_CHANGEPASSWORD_FAILED", ctx.getLoggerName());
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
/*
    // For Nashorn
    var directoryFilter = new FileFilter() {
			accept: function(file) {
				return file.isDirectory();
			}
		};
 */
    // For graal
    // var FileFilterAdapter = Java.extend(FileFilter);
    // var directoryFilter = new FileFilter();
    var directoryFilter = new (Java.extend(FileFilter, {
			accept: function(file) {
				return file.isDirectory();
			}
    }));

    ctx.service("auth").setInactiveActionPermission(ctx);
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
    ctx.service("auth").deleteInactiveActionPermission(ctx);
  },
  _processModule(module_name, ctx) {
    var objModule = ctx.module(module_name);
    if (objModule == null) {
      console.log("Module not found: " + module);
    }
    for (var i in objModule) {
      if (objModule[i] instanceof Function && !(i.startsWith('_')) && !(i == 'getLoggerName')) {
        var str_permission = `${module_name}.${i}`;
        ctx.service("auth").addActionPermission(str_permission, ctx);
        console.log(str_permission);
      }
    }
  }
});
