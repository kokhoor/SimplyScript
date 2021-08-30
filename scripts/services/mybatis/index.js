/* global moment, Java */
(function() {
  
function mybatis() {
}

mybatis.prototype = {
  _init() {
  },
  _setup(serviceName, args, system, path, ctx) {
    this._loggername = "services." + serviceName;
    log.info(this, "Service Name: {}, my path is: {}, args: {}", serviceName, path, args);
    ctx.addClasspath(path + 'dependency/');
    ctx.addClasspath(path + 'dependency/*.jar');
    const FileReader = Java.type('java.io.FileReader');
    this.configReader = new FileReader("config/mybatis/environment.xml");
    this.properties = new (Java.type('java.util.Properties'))();
    this.properties.load(new FileReader("config/mybatis/datasource.properties"));
    this.dbFactories = {};
    this.factoryBuilder = Java.type('org.apache.ibatis.session.SqlSessionFactoryBuilder');
    this.default_db = args['default'] || null;

/*
    let db = curry_pre([this], this.db);
    db.DB = this.DB;
    db.DB_NEW = this.DB_NEW;
 */
    return {
      contextPrototype: this,
      postInnerCall: this.postInnerCall,
      postCall: this.postCall          
    };
  },
  getLoggerName() {
    return this._loggername;
  },
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
  get(dbName, ctx) {
    log.info(this, "We are in the mybatis service.");
// console.log("in db: " + this.call + ":" + this.call + ":" + dbName + ":" + txType + ":" + this.DB + ":" + this.DB_NEW);
// print(me + ":" + dbName + ":" + txType + ":" + ctx);
// print(txType + ":" + this + ":" + this.DB + ":" + this.DB_NEW + ":" + this.DB + ":" + this.DB_NEW);
    if (ctx == null)
      throw new Error("Context must be provided");

    if (ctx._dbConn == null)
      ctx._dbConn = {};
 
    dbName = dbName || this.default_db;
    if (ctx._dbConn[dbName])
      return ctx._dbConn[dbName];

    var factory = this.dbFactories[dbName];
    if (factory == null) {
      factory = this.dbFactories[dbName] = (new this.factoryBuilder()).build(this.configReader, dbName, this.properties);
    }
    var db = ctx._dbConn[dbName] = factory.openSession();
    return db;
  },
  newDb(dbName, ctx) {
// console.log("in db: " + this.call + ":" + this.call + ":" + dbName + ":" + txType + ":" + this.DB + ":" + this.DB_NEW);
// print(me + ":" + dbName + ":" + txType + ":" + ctx);
// print(txType + ":" + this + ":" + this.DB + ":" + this.DB_NEW + ":" + this.DB + ":" + this.DB_NEW);
    if (ctx == null)
      throw new Error("Context must be provided");

    if (ctx._dbConnNew == null)
      ctx._dbConnNew = [];

    var factory = this.dbFactories[dbName];
    if (factory == null) {
      factory = this.dbFactories[dbName] = (new this.factoryBuilder()).build(this.configReader, dbName, this.properties);
    }

    var db = factory.openSession();
    ctx._dbConnNew.push(db);
    return db;
  },
  selectOne(dbName, scriptName, parameters, ctx) {
    var db = this.get(dbName || this.default_db, ctx);
    return db.selectOne(scriptName, parameters);
  },
  selectList(dbName, scriptName, parameters, ctx) {
    var db = this.get(dbName || this.default_db, ctx);
    return db.selectList(scriptName, parameters);
  },
  selectCursor(dbName, scriptName, parameters, ctx) {
    var db = this.get(dbName || this.default_db, ctx);
    return db.selectCursor(scriptName, parameters);
  },
  insert(dbName, scriptName, parameters, ctx) {
    var db = this.get(dbName || this.default_db, ctx);
    return db.insert(scriptName, parameters);
  },
  update(dbName, scriptName, parameters, ctx) {
    var db = this.get(dbName || this.default_db, ctx);
    return db.update(scriptName, parameters);
  },
  delete(dbName, scriptName, parameters, ctx) {
    var db = this.get(dbName || this.default_db, ctx);
    return db.delete(scriptName, parameters);
  }
};
var service = new mybatis();
service._init();
return service;

}());