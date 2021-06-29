/*
 * Copyright 2021 kokhoor.
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 *
 * @author kokhoor
 */
public class ScriptGlobalObject {
    WeakReference<ScriptEngine> engine = null;
    Map<String, Object> app = new ConcurrentHashMap<>();
    Cache<String, Object> cache = Caffeine.newBuilder()
                                    .maximumSize(1024)
                                    .build();
    Map<String, Object> system = new ConcurrentHashMap<>();
    Map<String, Object> services = new ConcurrentHashMap<>();
    Cache<String, Object> modules = Caffeine.newBuilder()
                                    .maximumSize(1024)
                                    .build();
    CompiledScript initScript = null;
    ScriptObjectMirror ctxConstructor = null;

    public ScriptGlobalObject(ScriptEngine engine, Map<String, Object> mapConfig) throws ScriptException {
        super();
        this.engine = new WeakReference(engine);
        ScriptContext currentCtx = new SimpleScriptContext();
        initScript = (CompiledScript)engine.engine().compile("load('scripts/init.js')");
        initScript.eval(currentCtx);

        ScriptObjectMirror fnCtxFactory = (ScriptObjectMirror)engine.eval("load('scripts/system/setup_env.js')", currentCtx);
        ScriptObjectMirror ctxFactoryRet = (ScriptObjectMirror)fnCtxFactory.call(fnCtxFactory);
        ctxConstructor = (ScriptObjectMirror)ctxFactoryRet;
        ctxFactoryRet.callMember("config", mapConfig);
    }

    public CompiledScript initScript() {
        return initScript;
    }
    
    public ScriptObjectMirror ctxConstructor() {
        return ctxConstructor;
    }

    public ScriptEngine engine() {
        return engine.get();
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
