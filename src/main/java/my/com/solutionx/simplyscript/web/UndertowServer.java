/*
 * Copyright 2021 SolutionX Software Sdn. Bhd. <info@solutionx.com.my>.
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
package my.com.solutionx.simplyscript.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.Handlers;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.script.ScriptException;
import my.com.solutionx.simplyscript.ScriptEngine;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;
import stormpot.PoolException;

public class UndertowServer {
    private ScriptEngine engine = null;
    Undertow server = null;
    private static final char[] STORE_PASSWORD = "password".toCharArray();

    public UndertowServer() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException, ScriptException,
            FileNotFoundException, PoolException, InterruptedException {
        this(null);
    }

    public UndertowServer(String iniFile) throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException, ScriptException,
            FileNotFoundException, PoolException, InterruptedException {
        String ini_filename = System.getProperty("config", iniFile);
        if (ini_filename == null || ini_filename.length() == 0) {
            ini_filename = "config.ini";
        }
        Wini ini = new Wini(new File(ini_filename));
        Section iniMain = ini.get("main");
        engine = new ScriptEngine();
        engine.init(iniMain);

        Section sectionWebServer = ini.get("webserver");
        String protocol = sectionWebServer.getOrDefault("protocol", "http");
        String host = sectionWebServer.getOrDefault("host", "localhost");
        String port = sectionWebServer.getOrDefault("port", "8080");
        String certs_path = sectionWebServer.getOrDefault("certs_path", "certs/");
        
        HttpHandler handlers = new BlockingHandler(Handlers.pathTemplate(false).add("/api/{module}/{method}", new ScriptCallHandler()));

        String bindAddress = System.getProperty("bind.address", host);
        Builder builder = Undertow.builder();
        if (protocol.equalsIgnoreCase("http2")) {
            System.out.println(certs_path + "keystore.jks");
            SSLContext sslContext = createSSLContext(loadKeyStore(certs_path + "keystore.jks"),
                    loadKeyStore(certs_path + "truststore.jks"));
            builder = builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpsListener(Integer.valueOf(port), bindAddress, sslContext);
        } else if (protocol.equalsIgnoreCase("https")) {
            SSLContext sslContext = createSSLContext(loadKeyStore(certs_path + "keystore.jks"),
                    loadKeyStore(certs_path + "truststore.jks"));
            builder = builder.addHttpsListener(Integer.valueOf(port), bindAddress, sslContext);
        } else if (protocol.equalsIgnoreCase("http")) {
            builder = builder.addHttpListener(Integer.valueOf(port), bindAddress);
        }
        builder = builder.setHandler(handlers);

        server = builder.build();
        server.start();
    }

    class ScriptCallHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getInputStream(), StandardCharsets.UTF_8));
            String inputJSONString = br.lines().collect(Collectors.joining());
            if (inputJSONString == null || inputJSONString.length() == 0) {
                Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
                if (queryParameters != null) {
                    Deque<String> queueInput = queryParameters.get("i");
                    if (queueInput != null)
                        inputJSONString = queueInput.getFirst();
                }
            }
            System.out.println(inputJSONString);
            Map<String, Object> mapArgs = null;
            if (inputJSONString != null && inputJSONString.length() > 0) {
                ObjectMapper mapper = new ObjectMapper();
                mapArgs = mapper.readValue(inputJSONString, Map.class);
            }

            PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
            String module = pathMatch.getParameters().get("module");
            String method = pathMatch.getParameters().get("method");
            Object response = null;
            try {
                 response = engine.actionReturnString(module + "." + method, mapArgs);
            } catch (Exception e) {
                String out = String.format("%s:%s:%s%n", "UndertowServer:Error calling action",
                        e.getMessage(), e.getClass().getName());
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> map = new HashMap<>();
                map.put("success", false);
                map.put("message", out);
                response = mapper.writeValueAsString(out);
            }
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(response.toString());
        }
    }
    
    private static KeyStore loadKeyStore(String name) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final InputStream stream = Files.newInputStream(Paths.get(name));

        if (stream == null) {
            throw new RuntimeException("Could not load keystore: " + name);
        }

        try(InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, STORE_PASSWORD);
            return loadedKeystore;
        }
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, STORE_PASSWORD);
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }
}
