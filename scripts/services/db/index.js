/* global moment, Java */
(function() {
  
function db() {
}

db.prototype = {
  init() {
    const FileReader = Java.type('java.io.FileReader');
    this.configReader = new FileReader("config/mybatis/environment.xml");
    this.properties = new (Java.type('java.util.Properties'))();
    this.properties.load(new FileReader("config/mybatis/datasource.properties"));
    this.dbFactories = {};
    this.factoryBuilder = Java.type('org.apache.ibatis.session.SqlSessionFactoryBuilder');
  },
  setup() {
    return {
      contextPrototypes: {
        DB: this.DB,
        DB_NEW: this.DB_NEW,
        db: curry_pre([this], this.db)
        /*(function (_this) {
          return function () {
            var fn = _this.db;
            var args = Array.prototype.slice.call(arguments);
            args.unshift(_this);
            return fn.apply( this, args);
          };
        })(this)*/
      },
      callbacks: {
        postInnerCall: this.postInnerCall,
        postCall: this.postCall          
      }
    };
  },
  DB: 0,
  DB_NEW: 1,
  postInnerCall(ctx, e) {
    if (ctx._dbConnNew == null)
      return;

    for (var i=ctx._dbConnNew.length-1; i>=0; i--) {
      var db = ctx._dbConnNew[i];
      if (db.depth < ctx.callDepth)
        break;

      if (e != null)
        db.rollback();
      else
        db.commit();
      db.close();
      print("postInnderCall Cleaning up DB_NEW db: " + db);
      ctx._dbConnNew.pop();
    }
  },
  postCall(ctx, e) {
    if (ctx._dbConnNew != null) {
      for (var i=ctx._dbConnNew.length-1; i>=0; i--) {
        var db = ctx._dbConnNew[i];
        if (e != null)
          db.rollback();
        else
          db.commit();
        db.close();
        print("postCall Cleaning up DB_NEW db: " + db);
      }
      ctx._dbConnNew[i] = null;
    }

    if (ctx._dbConn != null) {
      for (var dbName in ctx._dbConn) {
        var db = ctx._dbConn[dbName];
        if (e != null)
          db.rollback();
        else
          db.commit();
        db.close();
        print("postCall Cleaning up DB db: " + db);
        delete ctx._dbConn[dbName];
      }
    }
  },
  db(me, dbName, txType, ctx) {
// print(me + ":" + dbName + ":" + txType + ":" + ctx);
// print(txType + ":" + this + ":" + this.DB + ":" + this.DB_NEW + ":" + me.DB + ":" + me.DB_NEW);
    if (ctx == null)
      ctx = this;
    if (txType == null)
      txType = this.DB;
    switch (txType) {
      case this.DB:
        if (ctx._dbConn == null)
          ctx._dbConn = {};

       if (ctx._dbConn[dbName])
         return ctx._dbConn[dbName];

       var factory = me.dbFactories[dbName];
       if (factory == null) {
         factory = me.dbFactories[dbName] = (new me.factoryBuilder()).build(me.configReader, dbName, me.properties);
       }
       var db = ctx._dbConn[dbName] = factory.openSession();
       return db;
      case this.DB_NEW:
        if (ctx._dbConnNew == null)
          ctx._dbConnNew = [];

        var factory = me.dbFactories[dbName];
        if (factory == null) {
          factory = me.dbFactories[dbName] = (new me.factoryBuilder()).build(me.configReader, dbName, me.properties);
        }

        var db = factory.openSession();
        ctx._dbConnNew.push(db);
        return db;
      default:
        throw new Error("Unknown Database Transaction Type provided.");
    }
  }
};
var service = new db();
service.init();
print("In setup have factory?" + _.dbFactories);
return service;

}());