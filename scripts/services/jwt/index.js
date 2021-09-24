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
  
function jwt() {
}

jwt.prototype = {
  _init() {
  },
  _setup(serviceName, args, system, path, ctx) {
    this._loggername = "services." + serviceName;
    log.info(this, "Service Name: {}, my path is: {}, args: {}", serviceName, path, args);
    ctx.localContext.addClasspath(path + 'dependency/');
    ctx.localContext.addClasspath(path + 'dependency/*.jar');
    var secret = args.secret;
    if (secret == null || secret == '')
      raiseError("JWT key is not defined", "E_JWTINVALIDKEY", this._loggername + "._setup");

    this.secret = Java.type("java.nio.charset.StandardCharsets").UTF_8.encode(secret).array();
    this.issuer = args.issuer || 'solutionx';
    this.expiration = args.expiration || 30;

    this.JwtClaims = Java.type("org.jose4j.jwt.JwtClaims");
    this.HmacKey = Java.type("org.jose4j.keys.HmacKey");
    this.JsonWebSignature = Java.type("org.jose4j.jws.JsonWebSignature");
    this.AlgorithmIdentifiers = Java.type("org.jose4j.jws.AlgorithmIdentifiers");
    this.JwtConsumerBuilder = Java.type("org.jose4j.jwt.consumer.JwtConsumerBuilder");

    return {
      contextPrototype: this,
      preCall: {fn: this.preCall, priority: 12000}
    };
  },
  getLoggerName() {
    return this._loggername;
  },
  preCall(ctx, e, action, args) {
    var headers = ctx.req("headers");
    if (headers == null)
      return;
    
    var authorization = headers.getFirst("Authorization");
    if (authorization == null)
      return;

    var token_array = authorization.split(" ", 2);
    if (token_array.length < 2 || token_array[0] !== 'Bearer')
      return;
    
    var token = token_array[1];
    var key = new this.HmacKey(this.secret);
    var jwtConsumer = new this.JwtConsumerBuilder()
      .setRequireExpirationTime()
      .setAllowedClockSkewInSeconds(30)
      .setExpectedIssuer(this.issuer)        
      .setVerificationKey(key)
      .setExpectedType(true, "JWT")
      .build();
//    console.log(`Token: ${token}`);
    try {
      var claim = jwtConsumer.processToClaims(token);
      var username = claim.getStringClaimValue("username");
      var is_active = claim.getClaimValue("is_active");
      var is_staff = claim.getClaimValue("is_staff");
      var is_superuser = claim.getClaimValue("is_superuser");
      var dt = new (Java.type("java.util.Date"))(claim.getExpirationTime().getValueInMillis());
      ctx.req("user", {
        "username": username,
        "active": is_active,
        "staff": is_staff,
        "superuser": is_superuser,
        "expiry": dt
      });
    } catch (e) {
      throw e;
        console.log("Invalid JWT: " + e.message);
    }
  },
  encode(args) {
    var claims = new this.JwtClaims();
    claims.setIssuedAtToNow();  // when the token was issued/created (now)
    claims.setIssuer(this.issuer);  // who creates the token and signs it
    claims.setExpirationTimeMinutesInTheFuture(this.expiration); // time when the token will expire (30 minutes from now)
    claims.setGeneratedJwtId(); // a unique identifier for the token
    // claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
    for (var item in args) {
      claims.setClaim(item, args[item]);
    }
    
    var key = new this.HmacKey(this.secret);
    var jws = new this.JsonWebSignature();
    jws.setHeader("typ", "JWT");
    jws.setPayload(claims.toJson());
    jws.setAlgorithmHeaderValue(this.AlgorithmIdentifiers.HMAC_SHA256);
    jws.setKey(key);
    return jws.getCompactSerialization();
  }
};

var service = new jwt();
service._init();
return service;

}());