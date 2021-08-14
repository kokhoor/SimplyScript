({
  saveEmployee(args, ctx) {
    ctx.db.update('default', 'saveEmployee', {
      name: "Demo Test Employee 01",
      contactno: "do not contact me",
      emailaddress: "demotest01@demotest.com",
      mobileno: "12345"
    }, ctx);
    var db = ctx.db.get('default', ctx);
    var updated = db.update("saveEmployee", {
      name: "Demo Test Employee 01",
      contactno: "contact me!",
      emailaddress: "demotest01@demotest.com",
      mobileno: "12345"
    });
    return updated;
  },
  getEmployee(args, ctx) {
    return ctx.db.selectOne("default", "getEmployee", {"mobileno": "12345"}, ctx);
  },
  getEmployees(args, ctx) {
    var params = {
      "mobileno": ["mobileno", "12345"]
    };
    console.log("Params: " + params);
    return ctx.db.selectList("default", "getEmployees", params, ctx);
  },
  test(args, ctx) {    
    ctx.call("Alert.out", {"str": "String to display"});
    print("Before get db: " + ctx + ":" + ctx.db + ":" + ctx.db.get + ":" + ctx.db.newDb);
    try {
      var db = ctx.db.get('default', ctx);
    } catch (e) {
      print("Exception: " + e.message);
      throw e;
    }
    print("Have db: " + db);
    if (db != null) {
      var cursor = db.selectCursor("getEmployees",
        {"mobileno": ["mobileno", "12345"]});
      for (const row of cursor) {
        console.log(`${row.name}`)
      }
      cursor.close();
    }
    var db2 = ctx.db.newDb('default', ctx);
    print("Have db2: " + db2);
    var db_same = ctx.db.get('default', ctx);
    print("Have db_same: " + db_same);
    var db2_same = ctx.db.newDb('default', ctx);
    print("Have db2_same (shd be different): " + db2_same);
    return "CallTest.test completed!";
  },
  test2(args, ctx) {
    return {"a": 5, "b": 6, "c": [1,2,3], "d": {"x":0, "y": 10}};
  }
});