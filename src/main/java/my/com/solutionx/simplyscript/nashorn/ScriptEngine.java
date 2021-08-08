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
package my.com.solutionx.simplyscript.nashorn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.benmanes.caffeine.cache.Cache;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
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
 * @author SolutionX Software Sdn. Bhd. <info@solutionx.com.my>
 */
public class ScriptEngine implements ScriptEngineInterface {
    CompiledScript initScript = null;
    ScriptObjectMirror ctxConstructor = null;
    NashornScriptEngine engine = null;
    WeakReference<ScriptService> scriptService = null;

    @Override
    public void init(ScriptService scriptService, Map<String, Object> mapScriptConfig) throws ScriptException {
        this.scriptService = new WeakReference<>(scriptService);
        Map<String, String> config = (Map<String, String>) mapScriptConfig.get("config");
        String scripts_path = config.getOrDefault("scripts_path", "./scripts/");

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--optimistic-types=false", "--language=es6" });

        ScriptContext currentCtx = new SimpleScriptContext();
        initScript = (CompiledScript)engine.compile("load('" + scripts_path + "init.js')");
        initScript.eval(currentCtx);

        ScriptObjectMirror fnCtxFactory = (ScriptObjectMirror)engine.eval("load('" + scripts_path + "system/setup_env.js')", currentCtx);
        ScriptObjectMirror ctxFactoryRet = (ScriptObjectMirror)fnCtxFactory.call(fnCtxFactory);
        ctxConstructor = (ScriptObjectMirror)ctxFactoryRet;
        ctxFactoryRet.callMember("config", mapScriptConfig);
    }

    CompiledScript initScript() {
        return initScript;
    }

    ScriptObjectMirror ctxConstructor() {
        return ctxConstructor;
    }

    @Override
    public ScriptContextInterface getScriptContext() {
        return new NashornScriptContext(this);
    }

    @Override
    public Object eval(String script, ScriptContextInterface ctx) throws ScriptException {
        return engine.eval(script, (ScriptContext)ctx);
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
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror)scriptContext.getScriptContext().ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            return ctx.callMember("call", action, args);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    public String actionReturnString(String action, Object args) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            // ScriptObjectMirror ctxConstructor = (ScriptObjectMirror)scriptContext.getScriptContext().ctxConstructor();
            ScriptObjectMirror ctx = (ScriptObjectMirror)ctxConstructor.newObject(scriptContext.getScriptContext());
            Object ret = ctx.callMember("call", action, args);
            
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(new ScriptObjectMirrorSerializer(ScriptObjectMirror.class));
            // module.addSerializer(ScriptObjectMirror.class, new ScriptObjectMirrorSerializer());
            mapper.registerModule(module);
            return mapper.writeValueAsString(ret);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }
}
