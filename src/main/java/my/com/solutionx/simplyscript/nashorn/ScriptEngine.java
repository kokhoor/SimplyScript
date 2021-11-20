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
import org.openjdk.nashorn.internal.objects.NativeError;
import org.openjdk.nashorn.internal.runtime.ECMAException;
import org.openjdk.nashorn.internal.runtime.Undefined;
import stormpot.PoolException;
import stormpot.Timeout;

/**
 *
 * @author SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;
 */
public class ScriptEngine implements ScriptEngineInterface {
    CompiledScript initScript = null;
    ScriptObjectMirror ctxObject = null;
    ScriptObjectMirror ctxConstructor;
    NashornScriptEngine engine = null;
    WeakReference<ScriptService> scriptService = null;
    String scripts_path = null;
    String config_path = null;
    String working_path = null;

    @Override
    public void init(ScriptService scriptService, Map<String, Object> mapScriptConfig) throws ScriptException {
        this.scriptService = new WeakReference<>(scriptService);
        Map<String, String> config = (Map<String, String>) mapScriptConfig.get("config");
        scripts_path = config.get("scripts_path");
        config_path = config.get("config_path");
        working_path = config.get("working_path");

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--optimistic-types=false", "--language=es6" },
                scriptService.getClassLoader());

        NashornScriptContext currentCtx = (NashornScriptContext)getScriptContext();
        initScript = (CompiledScript)engine.compile("load('" + scripts_path + "init.js')");
        initScript.eval(currentCtx);

        ScriptObjectMirror fnCtxFactory = (ScriptObjectMirror)engine.eval("load('" + scripts_path + "system/ctx_prototype.js')", currentCtx);
        ScriptObjectMirror ctxFactoryRet = (ScriptObjectMirror)fnCtxFactory.call(fnCtxFactory);
        ctxObject = (ScriptObjectMirror) ctxFactoryRet.getSlot(0);
        ctxConstructor = (ScriptObjectMirror) ctxFactoryRet.getSlot(1);
        ctxObject.callMember("config", mapScriptConfig);
    }

    CompiledScript initScript() {
        return initScript;
    }

    ScriptObjectMirror ctxConstructor() {
        return ctxObject;
    }

    @Override
    public ScriptContextInterface getScriptContext() {
        NashornScriptContext scriptContext = new NashornScriptContext(this);
        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("scripts_path", scripts_path);
        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("config_path", config_path);
        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("working_path", working_path);
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
            // ScriptObjectMirror ctxObject = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxObject();
            // ScriptObjectMirror ctx = (ScriptObjectMirror)ctxObject.newObject(scriptContext.getScriptContext());
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.call(null, scriptContext.getScriptContext());
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
            // ScriptObjectMirror ctxObject = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxObject();
            //ScriptObjectMirror ctx = (ScriptObjectMirror)ctxObject.newObject(scriptContext.getScriptContext());
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.call(null, scriptContext.getScriptContext());
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
            // ScriptObjectMirror ctxObject = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxObject();
            //ScriptObjectMirror ctx = (ScriptObjectMirror)ctxObject.newObject(scriptContext.getScriptContext());
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.call(null, scriptContext.getScriptContext());
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
            // ScriptObjectMirror ctxObject = (ScriptObjectMirror) ((NashornScriptContext)scriptContext.getScriptContext()).ctxObject();
            // ScriptObjectMirror ctx = (ScriptObjectMirror)ctxObject.newObject(scriptContext.getScriptContext());
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.call(null, scriptContext.getScriptContext());
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

    public Map<String, Object> action(String action, Object args, Map<String, Object> mapReq) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        if (scriptContext == null) {
            throw new ScriptException("FTimeout trying to execute script");
        }
// System.out.println("Getting PoolableScriptContext: "+ scriptContext + ":" + scriptContext.getScriptContext());
        scriptContext.getScriptContext().setRequest(mapReq);

        try {
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
//            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(null, scriptContext.getScriptContext());
//System.out.println("ctx: " + ctx);
            Object ret = ctx.callMember("call", action, args);
            Map<String, Object> map = (Map<String, Object>) scriptContext.getScriptContext().req(OTHER_RETURN_DATA);
            if (map == null)
                map = new HashMap<>();
            if (ret != null && ret.getClass() != Undefined.class)
                map.put("data", ret);
            return map;
        } finally {
//System.out.print("in action finally: " + scriptContext);
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    @Override
    public String actionReturnString(String action, Object args, Map<String, Object> mapReq) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        try {
            Map<String, Object> ret = action(action, args, mapReq);
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(new ScriptObjectMirrorSerializer(ScriptObjectMirror.class));
            mapper.registerModule(module);

            ret.put("success", true);
            return mapper.writeValueAsString(ret);
        } catch (ECMAException e) {
            String out = e.getMessage();

            Map<String, Object> map = new HashMap<>();
            map.put("success", false);
            map.put("message", out);

            if (e.thrown != null && e.thrown instanceof NativeError) {
                NativeError ne = (NativeError)e.thrown;
                Object code = ne.get("code");
                if (code != null && code != Undefined.getUndefined())
                    map.put("code", code.toString());
                Object actionIn = ne.get("action");
                if (actionIn != null && actionIn != Undefined.getUndefined())
                    map.put("action", actionIn.toString());
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            String out = e.getMessage(); // String.format("%s:%s:%s%n", "Error calling action",
                    // e.getMessage(), e.getClass().getName());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = new HashMap<>();
            map.put("success", false);
            map.put("message", out);
            return mapper.writeValueAsString(map);
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
        if (ctxObject != null)
            ctxObject.clear();
        initScript = null;
        engine = null;
    }
}
