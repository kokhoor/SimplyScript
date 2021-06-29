/* global include, ctx */

var console = { 
    log: print,
    warn: print,
    error: print
};

String.prototype.delimit_quotes = function () {
    return this.replace(/'/g, "''");
};

print("Load moment");
load('scripts/lib/moment.min.js');
print("Load numeral");
load('scripts/lib/numeral.min.js');
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