(function() {

function camel() {
  this.data = 100;
}

camel.prototype = {
  sum(a,b) {
    return this.data + a + b;
  },
  subtract(a, b) {
    return a - b;
  }
};

return new camel();
}());