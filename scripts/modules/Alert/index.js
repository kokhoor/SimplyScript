({
    x: 5,
    out(str) {
        print(`Alert:
${str} ${this.x}`);
    },
    test() {
        print(`This is a test: ${"abc".startsWith("ab")} :
                I'm good, are you 'ok'? \"test\"`);
    }
});
