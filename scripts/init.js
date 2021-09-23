let log = {
  LoggerFactory: Java.type("org.slf4j.LoggerFactory"),
  _getLogger(where) {
    if (where instanceof Object && where.getLoggerName) {
      return this.LoggerFactory.getLogger(where.getLoggerName());
    } else {
      return this.LoggerFactory.getLogger(where);
    }
  },
  error(where, str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) {
    this._getLogger(where).error(str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  warn(where, str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) {
    this._getLogger(where).warn(str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  info(where, str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) {
    this._getLogger(where).info(str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  debug(where, str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) {
    this._getLogger(where).debug(str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);    
  },
  trace(where, str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) {
    this._getLogger(where).trace(str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  }
};

var console = { 
  log: (str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => {
    log.info("context", str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  error: (str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => {
    log.error("context", str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  warn: (str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => {
    log.warn("context", str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  info: (str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => {
    log.info("context", str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  debug: (str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => {
    log.debug("context", str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  },
  trace: (str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => {
    log.trace("context", str, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
  }
};

function raiseError(message, code, action) {
  var e = new Error(message);
  e.code = code;
  e.action = action;
  throw e;
};

function check_required(args, fields, ctx) {
  var error = "";
  fields.forEach((field) => {
    if (!(field in args)) {
      if (error.length > 0)
        error += ","
      error += field;
    }
  });
  if (error.length > 0) {
    raiseError(`Required field(s) ${error} not provided.`, "E_MISSINGARGS", ctx.getLoggerName());
  }
}

function check_empty(args, fields, ctx) {
  var error = "";
  fields.forEach((field) => {
    if (args[field] == null || args[field] == '') {
      if (error.length > 0)
        error += ","
      error += field;
    }
  });
  if (error.length > 0) {
    raiseError(`Required field(s) ${error} not provided or empty.`, "E_MISSINGARGSOREMPTY", ctx.getLoggerName());
  }
}

String.prototype.delimit_quotes = function () {
  return this.replace(/'/g, "''");
};

print("Load dayjs");
load(scripts_path + 'lib/dayjs.min.js');
load(scripts_path + 'lib/dayjs.customParseFormat.js');
dayjs.extend(dayjs_plugin_customParseFormat);
console.log(dayjs("12-25-1995", "MM-DD-YYYY").format("DD MMM YYYY HH:mm:ss"));
print("Load numeral");
load(scripts_path + 'lib/numeral.min.js');
print("Loaded all libs");

function curry_pre(arrPre, fn) {
  return function () {
    var args = Array.prototype.slice.call(arguments);
    args = arrPre.concat(args);
    return fn.apply(this, args);
  };
}

const global = {
    modules: {},
    math: load(scripts_path + 'lib/camel.js'),
    db: {
    },
    utils: {
      load(script) {
          return this;
      }
    },
    test: {
      exec(name) {
        return name;
      },
      sum(x, y) {
        return x + y;
      }
    }
};

print("Have math:");
print(global.math);
print("After have math");
print(global.test.sum(1,2));