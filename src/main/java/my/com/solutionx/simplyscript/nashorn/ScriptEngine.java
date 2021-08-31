/*
 * Copyright 2021 SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;.
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
package my.com.solutionx.simplyscript.nashorn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.benmanes.caffeine.cache.Cache;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import my.com.solutionx.simplyscript.PoolableScriptContext;
import my.com.solutionx.simplyscript.ScriptContextInterface;
import my.com.solutionx.simplyscript.ScriptEngineInterface;
import my.com.solutionx.simplyscript.ScriptService;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import stormpot.PoolException;
import stormpot.Timeout;

/**
 *
 * @author SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;
 */
public class ScriptEngine implements ScriptEngineInterface {
    CompiledScript initScript = null;
    ScriptObjectMirror ctxConstructor = null;
    NashornScriptEngine engine = null;
    WeakReference<ScriptService> scriptService = null;
    String scripts_path = null;
    String config_path = null;

    @Override
    public void init(ScriptService scriptService, Map<String, Object> mapScriptConfig) throws ScriptException {
        this.scriptService = new WeakReference<>(scriptService);
        Map<String, String> config = (Map<String, String>) mapScriptConfig.get("config");
        scripts_path = config.getOrDefault("scripts_path", "./scripts/");
        config_path = config.getOrDefault("config_path", "./config/");

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--optimistic-types=false", "--language=es6" },
                scriptService.getClassLoader());

        NashornScriptContext currentCtx = (NashornScriptContext)getScriptContext();
        initScript = (CompiledScript)engine.compile("load('" + scripts_path + "init.js')");
        initScript.eval(currentCtx);

        ScriptObjectMirror fnCtxFactory = (ScriptObjectMirror)engine.eval("load('" + scripts_path + "system/setup_env.js')", currentCtx);
        ScriptObjectMirror ctxFactoryRet = (ScriptObjectMirror)fnCtxFactory.call(fnCtxFactory);
        ctxConstructor = (ScriptObjectMirror)ctxFactoryRet;
        ctxConstructor.callMember("config", mapScriptConfig);
    }

    CompiledScript initScript() {
        return initScript;
    }

    ScriptObjectMirror ctxConstructor() {
        return ctxConstructor;
    }

    @Override
    public ScriptContextInterface getScriptContext() {
        NashornScriptContext scriptContext = new NashornScriptContext(this);
        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("scripts_path", scripts_path);
        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("config_path", config_path);
        return scriptContext;
    }

    @Override
    public Object eval(String script, ScriptContextInterface ctx) throws ScriptException {
        return engine.eval(script, (ScriptContext)ctx);
    }
 
    public void loadServices(List<String> services) throws ScriptException, PoolException, InterruptedException {
        if (services == null || services.isEmpty())
            return;

        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            services.forEach(name -> {
                ctx.callMember("service", name);
            });
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public Object getService(String name) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            return ctx.callMember("service", name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public void loadModules(List<String> modules) throws ScriptException, PoolException, InterruptedException {
        if (modules == null || modules.isEmpty())
            return;

        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            modules.forEach(name -> {
                ctx.callMember("module", name);
            });
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public Object getModule(String name) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            return ctx.callMember("module", name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public Map<String, Object> app() {
        return scriptService.get().app();
    }
    
    public Cache<String, Object> cache() {
        return scriptService.get().cache();
    }

    public Map<String, Object> system() {
        return scriptService.get().system();
    }

    public Map<String, Object> services() {
        return scriptService.get().services();
    }

    public Cache<String, Object> modules() {
        return scriptService.get().modules();
    }

    public Object app(String key) {
        return scriptService.get().app(key);
    }
    
    public Object app(String key, Object value) {
        return scriptService.get().app(key, value);
    }

    public Object cache(String key) {
        return scriptService.get().cache(key);
    }
    
    public void cache(String key, Object value) {
        scriptService.get().cache(key, value);
    }

    public Object system(String key) {
        return scriptService.get().system(key);
    }
    
    public Object system(String key, Object value) {
        return scriptService.get().system(key, value);
    }

    public Object service(String key) {
        return scriptService.get().service(key);
    }
    
    public Object service(String key, Object value) {
        return scriptService.get().service(key, value);
    }

    public Object module(String key) {
        return scriptService.get().module(key);
    }
    
    public void module(String key, Object value) {
        scriptService.get().module(key, value);
    }

    public Object action(String action, Object args) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            return ctx.callMember("call", action, args);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public String actionReturnString(String action, Object args) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        try {
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror)scriptContext.getScriptContext().ctxConstructor();
            Object ret = action(action, args);
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(new ScriptObjectMirrorSerializer(ScriptObjectMirror.class));
            // module.addSerializer(ScriptObjectMirror.class, new ScriptObjectMirrorSerializer());
            mapper.registerModule(module);

            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            map.put("data", ret);
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            String out = String.format("%s:%s:%s%n", "Error calling action",
                    e.getMessage(), e.getClass().getName());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = new HashMap<>();
            map.put("success", false);
            map.put("message", out);
            return mapper.writeValueAsString(out);
        }
    }

    @Override
    public void addClasspath(String path) throws MalformedURLException {
        scriptService.get().addClasspath(path);
    }

    @Override
    public void shutdown() {
        if (scriptService != null)
            scriptService.clear();
        scriptService = null;
        if (ctxConstructor != null)
            ctxConstructor.clear();
        initScript = null;
        engine = null;
    }
}
