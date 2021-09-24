({
    login(args, ctx) {
      check_empty(args, ['username', 'password'], ctx);
      var user = ctx.service("auth").verifyUserPassword(args.username, args.password, ctx);
      if (user != null) {
        var token = ctx.service("jwt").encode(user);
        ctx.setReturn("token", token);
      }
      return user;
    },
    changePassword(args, ctx) {
      if (!ctx.req("user"))
        raiseError("Not logged in", "E_NOTLOGGEDIN", ctx.getLoggerName());

      check_empty(args, ['current', 'new', 'confirm'], ctx);

      if (args.current == args.new)
        raiseError("New password cannot be the same as Confirm password", "E_CHANGEPASSWORD_SAMECURRENT_NEW", ctx.getLoggerName());        
      if (args.new != args.confirm)
        raiseError("New password and Confirm password must match", "E_CHANGEPASSWORD_NOTMATCH_CURRENT_NEW", ctx.getLoggerName());        

      var user = ctx.service("auth").verifyUserPassword(ctx.req("user").username, args.current, ctx);
      if (user == null)
        raiseError("Current password is not valid", "E_CHANGEPASSWORD_INVALIDCURRENTPASSWORD", ctx.getLoggerName());        

      var ret = ctx.service("auth").setPassword(ctx.req("user").username, args.new, ctx);
      if (ret == 1)
        return;
      else if (ret > 1) {
        raiseError("Abnormal error. Change password will affect more than one user. Aborted", "E_CHANGEPASSWORD_MORETHANONERECORD", ctx.getLoggerName());
      } else
        raiseError("Failed to change password", "E_CHANGEPASSWORD_FAILED", ctx.getLoggerName());
    }
});
