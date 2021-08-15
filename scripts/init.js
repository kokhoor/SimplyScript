/* global include, ctx */

var console = { 
    log: print,
    warn: print,
    error: print
};

String.prototype.delimit_quotes = function () {
    return this.replace(/'/g, "''");
};

print("Load dayjs");
load('scripts/lib/dayjs.min.js');
load('scripts/lib/dayjs.customParseFormat.js');
dayjs.extend(dayjs_plugin_customParseFormat);
console.log(dayjs("12-25-1995", "MM-DD-YYYY").format("DD MMM YYYY HH:mm:ss"));
print("Load numeral");
load('scripts/lib/numeral.min.js');
print("Loaded all libs");

let log = {
  LoggerFactory: Java.type("org.slf4j.LoggerFactory"),
  _getLogger(where) {
    if (where instanceof Object && where.getLoggerName) {
      return this.LoggerFactory.getLogger(where.getLoggerName());
    } else {
console.log(where);
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

function curry_pre(arrPre, fn) {
  return function () {
    var args = Array.prototype.slice.call(arguments);
    args = arrPre.concat(args);
    return fn.apply(this, args);
  };
}

const global = {
    modules: {},
    math: load('scripts/lib/camel.js'),
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