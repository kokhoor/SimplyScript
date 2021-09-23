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
package my.com.solutionx.simplyscript.graal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.benmanes.caffeine.cache.Cache;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptException;
import my.com.solutionx.simplyscript.PoolableScriptContext;
import my.com.solutionx.simplyscript.ScriptContextInterface;
import my.com.solutionx.simplyscript.ScriptEngineInterface;
import my.com.solutionx.simplyscript.ScriptService;
import stormpot.PoolException;

import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import stormpot.Timeout;

/**
 *
 * @author SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;
 */
public class ScriptEngine implements ScriptEngineInterface {
    WeakReference<ScriptService> scriptService = null;
    Engine engine = null;
    ScriptContext ctx = null;
    Source initScript = null;
    private Value ctxConstructor;
    HostAccess hostAccess = null;
    String scripts_path = null;
    String config_path = null;

    @Override
    public void init(ScriptService scriptService, Map<String, Object> mapScriptConfig) throws ScriptException, IOException {
        this.scriptService = new WeakReference<>(scriptService);
        Map<String, String> config = (Map<String, String>) mapScriptConfig.get("config");
        scripts_path = config.getOrDefault("scripts_path", "./scripts/");
        config_path = config.getOrDefault("config_path", "./config/");

        initScript = Source.newBuilder("js", new File(scripts_path + "init.js")).build();
        engine = Engine.create();
        
        HostAccess.Builder builder = HostAccess.newBuilder();
        builder.targetTypeMapping(Value.class, Object.class, (v) -> {
                    return v.hasArrayElements();
                }, (v) -> {
                    return v.as(List.class);
                })
                .allowAllClassImplementations(true)
                .allowAllImplementations(true)
                .allowArrayAccess(true)
                .allowBufferAccess(true)
                .allowIterableAccess(true)
                .allowIteratorAccess(true)
                .allowListAccess(true)
                .allowMapAccess(true)
                .allowPublicAccess(true);
        hostAccess = builder.build();
        ctx = (ScriptContext)getScriptContext();
        ctx.init();
        
/*
        ctx = Context.newBuilder("js").engine(engine)
                .allowAllAccess(true)
                .allowValueSharing(true)
                .allowHostAccess(hostAccess)
                .allowIO(true).build();
        ctx.eval(initScript);
*/
        Source setupEnvScript = Source.newBuilder("js", new File(scripts_path + "system/setup_env.js")).build();
        Value value = ctx.ctx.eval(setupEnvScript);
//        ctxConstructor = (ScriptObjectMirror)ctxFactoryRet;
        ctxConstructor = value.execute();
        ctxConstructor.getMember("config").execute(mapScriptConfig);
    }

    @Override
    public ScriptContextInterface getScriptContext() {
        ScriptContext scriptContext = new ScriptContext(this, hostAccess, scriptService.get().getClassLoader());
        Value jsBindings = scriptContext.ctx.getBindings("js");
        jsBindings.putMember("scripts_path", scripts_path);
        jsBindings.putMember("config_path", config_path);
        // scriptContext.ctx.eval("js", "var scripts_path='" + scripts_path + "'");
        return scriptContext;
    }

    @Override
    public Object eval(String script, ScriptContextInterface ctx) throws ScriptException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getService(String name) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            Value ctx = (Value)ctxConstructor.newInstance(scriptContext.getScriptContext());
            Value callable = ctx.getMember("service");
            return callable.execute(name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    @Override
    public void loadServices(List<String> services) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            Value ctx = (Value)ctxConstructor.newInstance(scriptContext.getScriptContext());
            Value callable = ctx.getMember("service");
            for (String name : services)
                callable.execute(name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    @Override
    public void loadModules(List<String> modules) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            Value ctx = (Value)ctxConstructor.newInstance(scriptContext.getScriptContext());
            Value callable = ctx.getMember("module");
            for (String name : modules)
                callable.execute(name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    @Override
    public Object getModule(String name) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        try {
            Value ctx = (Value)ctxConstructor.newInstance(scriptContext.getScriptContext());
            Value callable = ctx.getMember("module");
            return callable.execute(name);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    @Override
    public Object action(String action, Object args, Map<String,Object> mapReq) throws ScriptException, PoolException, InterruptedException {
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        PoolableScriptContext scriptContext = scriptService.get().getScriptContextPool().claim(timeout);
        scriptContext.getScriptContext().setRequest(mapReq);

        try {
            Value ctx = (Value)ctxConstructor.newInstance(scriptContext.getScriptContext());
            Value callable = ctx.getMember("call");
            return callable.execute(action, args);
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
    }

    @Override
    public String actionReturnString(String action, Object args, Map<String,Object> mapReq) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        try {
            Object ret = action(action, args, mapReq);

            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(new ValueSerializer(Value.class));
            mapper.registerModule(module);

            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            map.put("data", ret);
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

    Value ctxConstructor() {
        return ctxConstructor;
    }
    
    public void addClasspath(String path)  throws MalformedURLException {
        scriptService.get().addClasspath(path);
    }

    @Override
    public void shutdown() {
        if (scriptService != null)
            scriptService.clear();
        scriptService = null;
        if (ctx != null)
            ctx.cleanup();
        ctx = null;
        initScript = null;
        ctxConstructor = null;
        hostAccess = null;
        if (engine != null)
            engine.close();
    }
}
