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
package my.com.solutionx.simplyscript;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import stormpot.Pool;
import stormpot.PoolBuilder;
import stormpot.PoolException;
import stormpot.Timeout;

/**
 *
 * @author kokhoor
 */
public class ScriptEngine {
    NashornScriptEngine engine;

    Pool<PoolableScriptContext> poolContext;
    Cache<String, Object> modules = Caffeine.newBuilder()
            .maximumSize(1024)
            .build();

    Map<String, Object> app = new ConcurrentHashMap<>();
    Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(1024)
            .build();
    Map<String, Object> system = new ConcurrentHashMap<>();
    Map<String, Object> services = new ConcurrentHashMap<>();
    CompiledScript initScript = null;
    ScriptObjectMirror ctxConstructor = null;

    public ScriptEngine() throws ScriptException {
        super();
    }
    
    public void init(Map<String, String> config) throws FileNotFoundException, IOException, ScriptException, PoolException, InterruptedException {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--optimistic-types=false", "--language=es6" });

        String config_path = config.getOrDefault("config_path", "./config/");
        String working_path = config.getOrDefault("working_path", "./");
        String scripts_path= config.getOrDefault("scripts_path", "./scripts/");
        String pool_size = config.getOrDefault("pool_size", "5");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> mapModuleConfig = mapper.readValue(new FileReader(config_path + "/scripts/module_conf.json"),
                Map.class);
        Map<String, Object> mapServiceConfig = mapper.readValue(new FileReader(config_path + "/scripts/service_conf.json"),
                Map.class);
        Map<String, Object> mapScriptConfig = new HashMap<>();
        mapModuleConfig.put("path", scripts_path + mapModuleConfig.getOrDefault("path", "modules"));
        mapServiceConfig.put("path", scripts_path + mapServiceConfig.getOrDefault("path", "services"));
        mapScriptConfig.put("module", mapModuleConfig);
        mapScriptConfig.put("service", mapServiceConfig);

        ScriptContext currentCtx = new SimpleScriptContext();
        initScript = (CompiledScript)engine.compile("load('" + scripts_path + "init.js')");
        initScript.eval(currentCtx);

        ScriptObjectMirror fnCtxFactory = (ScriptObjectMirror)engine.eval("load('" + scripts_path + "system/setup_env.js')", currentCtx);
        ScriptObjectMirror ctxFactoryRet = (ScriptObjectMirror)fnCtxFactory.call(fnCtxFactory);
        ctxConstructor = (ScriptObjectMirror)ctxFactoryRet;
        ctxFactoryRet.callMember("config", mapScriptConfig);

        PoolableScriptContextAllocator allocator = new PoolableScriptContextAllocator(this);
        PoolBuilder<PoolableScriptContext> poolBuilder = Pool.from(allocator);
        poolBuilder = poolBuilder.setSize(Integer.valueOf(pool_size));
        poolContext = poolBuilder.build();

        Timeout timeout = new Timeout(60, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = poolContext.claim(timeout);
        try {
            ScriptLocalObject ctx = scriptContext.getScriptContext();
            List<String> preload = (List<String>)mapServiceConfig.get("preload");
            if (preload != null) {
                for (String service : preload) {
                    ctx.service(service);
                }
            }

            preload = (List<String>)mapModuleConfig.get("preload");
            if (preload != null) {
                for (String service : preload) {
                    ctx.module(service);
                }
            }
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public CompiledScript initScript() {
        return initScript;
    }
    
    public ScriptObjectMirror ctxConstructor() {
        return ctxConstructor;
    }

    public NashornScriptEngine engine() {
        return engine;
    }

    public Map<String, Object> app() {
        return app;
    }
    
    public Cache<String, Object> cache() {
        return cache;
    }

    public Map<String, Object> system() {
        return system;
    }

    public Map<String, Object> services() {
        return services;
    }

    public Cache<String, Object> modules() {
        return modules;
    }

    public Object app(String key) {
        return app.get(key);
    }
    
    public Object app(String key, Object value) {
        if (value == null)
            return app.remove(key);
        return app.put(key, value);
    }

    public Object cache(String key) {
        return cache.getIfPresent(key);
    }
    
    public void cache(String key, Object value) {
        if (value == null) {
            cache.invalidate(key);
            return;
        }
        cache.put(key, value);
    }

    public Object system(String key) {
        return system.get(key);
    }
    
    public Object system(String key, Object value) {
        if (value == null)
            return system.remove(key);
        return system.put(key, value);
    }

    public Object service(String key) {
        return services.get(key);
    }
    
    public Object service(String key, Object value) {
        if (value == null)
            return services.remove(key);
        return services.put(key, value);
    }

    public Object module(String key) {
        return modules.getIfPresent(key);
    }
    
    public void module(String key, Object value) {
        if (value == null) {
            modules.invalidate(key);
            return;
        }
        modules.put(key, value);
    }
    
    public Object eval(String script, ScriptContext ctx) throws ScriptException {
        return engine.eval(script, ctx);
    }

    public Object getService(String name) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = poolContext.claim(timeout);
        try {
            ScriptObjectMirror ctxConstructor = (ScriptObjectMirror)scriptContext.getScriptContext().ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            return ctx.callMember("service", name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public Object action(String action) throws ScriptException, PoolException, InterruptedException {
        return action(action, null);
    }

    public Object action(String action, Object args) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = poolContext.claim(timeout);
        try {
            ScriptObjectMirror ctxConstructor = (ScriptObjectMirror)scriptContext.getScriptContext().ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            return ctx.callMember("call", action, args);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public Pool<PoolableScriptContext> getScriptContextPool() {
        return poolContext;
    }

    public void cleanup() {
        engine = null;
        app.clear();
        app = null;
        cache.invalidateAll();
        cache = null;
        system.clear();
        system = null;
        services.clear();
        services = null;
        modules.invalidateAll();
        modules = null;
    }
}
