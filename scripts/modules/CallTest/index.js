({
  test(args, ctx) {    
    ctx.call("Alert.out", {"str": "String to display"});
    print("Before get db: " + ctx + ":" + ctx.db + ":" + ctx.db.DB + ":" + ctx.db.DB_NEW);
    try {
      var db = ctx.db('test', ctx.db.DB);
    } catch (e) {
      print("Exception: " + e.message);
      throw e;
    }
    print("Have db: " + db);
    if (db != null) {
      var cursor = db.selectCursor("selectUser");
      for (const row of cursor) {
        console.log(`${row.username}`)
      }
      cursor.close();
    }
    var db2 = ctx.db('test', ctx.db.DB_NEW);
    print("Have db2: " + db2);
    var db_same = ctx.db('test', ctx.db.DB);
    print("Have db_same: " + db_same);
    var db2_same = ctx.db('test', ctx.db.DB_NEW);
    print("Have db2_same (shd be different): " + db2_same);
    return "CallTest.test completed!";
  },
  test2(args, ctx) {
    return {"a": 5, "b": 6, "c": [1,2,3], "d": {"x":0, "y": 10}};
  }
});