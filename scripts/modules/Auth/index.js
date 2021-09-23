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
      
    }
});
