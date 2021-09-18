package my.com.solutionx.simplyscript.auth;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Date;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.junit.Test;

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

/**
 *
 * @author SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;
 */
public class JWTTest {
    @Test
    public void testCreateJWT() throws JoseException, UnsupportedEncodingException, MalformedClaimException {
        String secret = "G'+?)D+}LkP#qE5FVSFG{vW6$:ZM8pGp";
        String token;

        {
            JwtClaims claims = new JwtClaims();
            claims.setIssuedAtToNow();  // when the token was issued/created (now)
            claims.setIssuer("solutionx");  // who creates the token and signs it
            claims.setExpirationTimeMinutesInTheFuture(30); // time when the token will expire (30 minutes from now)
            claims.setGeneratedJwtId(); // a unique identifier for the token
            // claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
            claims.setClaim("userid","18"); // ad
            claims.setClaim("email","kokhoor@gmail.com"); // ad

            Key key = new HmacKey(secret.getBytes("UTF-8"));
            JsonWebSignature jws = new JsonWebSignature();

            // The payload of the JWS is JSON content of the JWT Claims
            jws.setHeader("typ", "JWT");
            jws.setPayload(claims.toJson());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
            // The JWT is signed using the private key
            jws.setKey(key);
            token = jws.getCompactSerialization();
            System.out.println(token);
        }

        {
            Key key = new HmacKey(secret.getBytes("UTF-8"));
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setExpectedIssuer("solutionx")        
                .setVerificationKey(key)
                .setExpectedType(true, "JWT")
                .build();
            
            try {
                JwtClaims processedClaims = jwtConsumer.processToClaims(token);
                System.out.println(processedClaims);
                Date dt = new Date(processedClaims.getExpirationTime().getValueInMillis());
                System.out.println(dt);
            } catch (InvalidJwtException e) {
                System.out.println(e);
            }
        }
    }    
}
