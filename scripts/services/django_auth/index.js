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

(function() {
  
function django_auth() {
}

django_auth.prototype = {
  _init() {
  },
  _setup(serviceName, args, system, path, ctx) {
    this._loggername = "services." + serviceName;
    log.info(this, "Service Name: {}, my path is: {}, args: {}", serviceName, path, args);

    var db_service = args.db_service || "db";
    this.db = ctx.service(db_service);

    this.db_name = args.db_name || null;
    var dbFactory = this.db.getFactory(this.db_name);
    var conf = dbFactory.getConfiguration();
    var filepath = path + "mapper/auth.xml";
    var reader = new (Java.type("java.io.FileReader"))(filepath);
    var builder = new (Java.type("org.apache.ibatis.builder.xml.XMLMapperBuilder"))(reader, conf, filepath, conf.getSqlFragments());
    builder.parse();

    ctx.localContext.addClasspath(path + 'dependency/');
    ctx.localContext.addClasspath(path + 'dependency/*.jar');
    this.encoder_decoder = Java.type('my.com.solutionx.simplyscript_module.django_auth.PasswordEncoderDecoder');

    return {
      contextPrototype: this
    };
  },
  getLoggerName() {
    return this._loggername;
  },
  setPassword(username, password, ctx) {
    var hashed_password = this.encoder_decoder.encode(password);
    return this.db.update(this.db_name, "auth.updatePassword", {
      "username": username,
      "password": hashed_password
    }, ctx);
  },
  verifyUserPassword(username, password, ctx) {
    var user_record = this.db.selectOne(this.db_name, "auth.getUserInfo", {
      "username": username
    }, ctx);
    if (user_record == null)
      raiseError("User not found", "E_USERNOTFOUND", this.getLoggerName() + ".verifyPassword");
    if (!this.encoder_decoder.verifyPassword(password, user_record.password)) {
      raiseError("Invalid User Id or Password", "E_USERNAMEPASSWORDINCORRECT",
      this.getLoggerName() + ".verifyPassword");
    }
    user_record.remove("password");
    return user_record;
  }
};

var service = new django_auth();
service._init();
return service;

}());