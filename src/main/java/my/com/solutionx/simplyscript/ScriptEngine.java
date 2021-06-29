/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.com.solutionx.simplyscript;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import stormpot.Pool;
import stormpot.PoolException;
import stormpot.Timeout;

public final class ScriptEngine {
    NashornScriptEngineFactory factory;
    NashornScriptEngine engine;
    ScriptGlobalObject globalCtx;
    Pool<PoolableScriptContext> poolContext;
    Cache<String, Object> modules = Caffeine.newBuilder()
      .maximumSize(1024)
      .build();

    public ScriptEngine() throws ScriptException, IOException, PoolException, InterruptedException {
        factory = new NashornScriptEngineFactory();
        engine = (NashornScriptEngine) factory.getScriptEngine(new String[] { "--optimistic-types=false", "--language=es6" });

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> mapModuleConfig = mapper.readValue(new FileReader("config/scripts/module_conf.json"),
                Map.class);
        Map<String, Object> mapServiceConfig = mapper.readValue(new FileReader("config/scripts/service_conf.json"),
                Map.class);
        Map<String, Object> mapConfig = new HashMap<>();
        mapConfig.put("module", mapModuleConfig);
        mapConfig.put("service", mapServiceConfig);

        globalCtx = new ScriptGlobalObject(this, mapConfig);
        PoolableScriptContextAllocator allocator = new PoolableScriptContextAllocator(globalCtx);
        poolContext = Pool.from(allocator).build();


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
/*
            scriptCtx.
            Object ret = engine.eval("moment(new Date()).format('DD-MMM-YYYY')", scriptCtx);
            System.out.println(ret);
            ret = engine.eval("numeral('1000.3333').format('0,0.00');", scriptCtx);
            System.out.println(ret);

            engine.eval("moment(new Date()).format('DD-MMM-YYYY')", scriptCtx);
*/
        } finally {
            if (scriptContext != null) {
              scriptContext.release();
            }
        }
/*
        Object ret = getService("db");
        System.out.printf("Service: %s%n", ret);
        ret = action("CallTest.test", null);
        try {
            System.out.printf("Action:CallTest.test:%s%n", ret);
        } catch (Exception e) {
            System.out.printf("Exception: %s%n", e.getMessage());
            e.printStackTrace();
        }
*/
    }
    
    public NashornScriptEngine engine() {
        return engine;
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
}
