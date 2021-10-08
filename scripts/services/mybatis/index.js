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
    this.configReader = new FileReader(`${config_path}/mybatis/environment.xml`);
    this.properties = new (Java.type('java.util.Properties'))();
    this.properties.load(new FileReader(`${config_path}/mybatis/datasource.properties`));
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
      postInnerCall: {fn: this.postInnerCall, priority: 10000},
      postCall: {fn: this.postCall, priority: 10000}
    };
  },
  getLoggerName() {
    return this._loggername;
  },
  postInnerCall(ctx, e) {
    if (ctx.req("_dbConnNew") == null)
      return;

    for (var i=ctx.req("_dbConnNew").length-1; i>=0; i--) {
      var db = ctx.req("_dbConnNew")[i];
      if (db.depth < ctx.callDepth)
        break;

      if (e != null)
        db.rollback();
      else
        db.commit();
      db.close();
      print("postInnderCall Cleaning up DB_NEW db: " + db);
      ctx.req("_dbConnNew").pop();
    }
  },
  postCall(ctx, e) {
    if (ctx.req("_dbConnNew") != null) {
      for (var i=ctx.req("_dbConnNew").length-1; i>=0; i--) {
        var db = ctx.req("_dbConnNew")[i];
        if (e != null)
          db.rollback();
        else
          db.commit();
        db.close();
        print("postCall Cleaning up DB_NEW db: " + db);
      }
      ctx.req("_dbConnNew")[i] = null;
    }

    if (ctx.req("_dbConn") != null) {
      for (var dbName in ctx.req("_dbConn")) {
        var db = ctx.req("_dbConn")[dbName];
        if (e != null)
          db.rollback();
        else
          db.commit();
        db.close();
        print("postCall Cleaning up DB db: " + db);
        delete ctx.req("_dbConn")[dbName];
      }
    }
  },
  getFactory(dbName) {
    dbName = dbName || this.default_db;
    var factory = this.dbFactories[dbName];
    if (factory == null) {
      factory = this.dbFactories[dbName] = (new this.factoryBuilder()).build(this.configReader, dbName, this.properties);
    }
    return factory;
  },
  get(dbName, ctx) {
    log.info(this, "We are in the mybatis service.");
// console.log("in db: " + this.call + ":" + this.call + ":" + dbName + ":" + txType + ":" + this.DB + ":" + this.DB_NEW);
// print(me + ":" + dbName + ":" + txType + ":" + ctx);
// print(txType + ":" + this + ":" + this.DB + ":" + this.DB_NEW + ":" + this.DB + ":" + this.DB_NEW);
    if (ctx == null)
      throw new Error("Context must be provided");

    if (ctx.req("_dbConn") == null)
      ctx.req("_dbConn", {});
 
    dbName = dbName || this.default_db;
    if (ctx.req("_dbConn")[dbName])
      return ctx.req("_dbConn")[dbName];

    var factory = this.getFactory(dbName);
    var db = ctx.req("_dbConn")[dbName] = factory.openSession();
    return db;
  },
  newDb(dbName, ctx) {
// console.log("in db: " + this.call + ":" + this.call + ":" + dbName + ":" + txType + ":" + this.DB + ":" + this.DB_NEW);
// print(me + ":" + dbName + ":" + txType + ":" + ctx);
// print(txType + ":" + this + ":" + this.DB + ":" + this.DB_NEW + ":" + this.DB + ":" + this.DB_NEW);
    if (ctx == null)
      throw new Error("Context must be provided");

    if (ctx.req("_dbConnNew") == null)
      ctx.req("_dbConnNew", []);

    dbName = dbName || this.default_db;

    var factory = this.getFactory(dbName);
    var db = factory.openSession();
    ctx.req("_dbConnNew").push(db);
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

return mybatis;

}());