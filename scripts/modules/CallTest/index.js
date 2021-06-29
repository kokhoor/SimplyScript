({
  test(args, ctx) {    
    ctx.call("Alert.out", {"str": "String to display"});
    print("Before get db");
    try {
      var db = ctx.db('test', ctx.DB);
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
    var db2 = ctx.db('test', ctx.DB_NEW);
    print("Have db2: " + db2);
    var db_same = ctx.db('test', ctx.DB);
    print("Have db_same: " + db_same);
    var db2_same = ctx.db('test', ctx.DB_NEW);
    print("Have db2_same (shd be different): " + db2_same);
    return "CallTest.test completed!";
  }
});