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

(function() {
  
function metrics() {
}

metrics.prototype = {
  _init() {
  },
  _setup(serviceName, args, system, path, ctx) {
    this._loggername = "services." + serviceName;
    var db_service = args.db_service || "db";
    this.db = ctx.service(db_service);

    this.db_name = args.db_name || null;
    var dbFactory = this.db.getFactory(this.db_name);
    var conf = dbFactory.getConfiguration();
    // console.log("Database id: " + conf.	getDatabaseId());
    var filepath = path + "mapper/metrics.xml";
    var reader = new (Java.type("java.io.FileReader"))(filepath);
    var builder = new (Java.type("org.apache.ibatis.builder.xml.XMLMapperBuilder"))(reader, conf, filepath, conf.getSqlFragments());
    builder.parse();

    this.table_name = args.table_name || "metrics_stats";
    var create_table = args.create_table || true;
    if (create_table) {
      this.db.update(this.db_name, "metrics.createMetricsTable", {
          "table_name": this.table_name
        }, ctx);
      this.db.postCall(ctx);
    }

    return {
      contextPrototype: this,
      preCall: {fn: this.preCall, priority: 9000, "this": this },
      postCall: {fn: this.postCall, priority: 9000, "this": this }
    };
  },
  getLoggerName() {
    return this._loggername;
  },
  preCall(ctx, e) {
    ctx.start_time = (new Date()).getTime();
  },
  postCall(ctx, e) {
    console.log("Metrics table name: " + this.table_name);
    var time_taken_ms = (new Date()).getTime() - ctx.start_time;
    ctx.db.update(this.db_name, 'metrics.updateMetrics', {
      table_name: this.table_name,
      name: ctx.getLoggerName(),
      time_taken_ms: time_taken_ms
    }, ctx);
  }
};
var service = new metrics();
service._init();
return service;

}());