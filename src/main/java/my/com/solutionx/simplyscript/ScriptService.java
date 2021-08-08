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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.ScriptException;
import stormpot.Pool;
import stormpot.PoolBuilder;
import stormpot.PoolException;

/**
 *
 * @author kokhoor
 */
public class ScriptService {
    ScriptEngineInterface engine;

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

    public ScriptService() throws ScriptException {
        super();
    }
    
    public void init(Map<String, String> config) throws FileNotFoundException, IOException, ScriptException, PoolException, InterruptedException, ScriptServiceException, InvocationTargetException {
        String config_path = config.getOrDefault("config_path", "./config/");
        String working_path = config.getOrDefault("working_path", "./");
        String scripts_path= config.getOrDefault("scripts_path", "./scripts/");
        String pool_size = config.getOrDefault("pool_size", "5");

        String ScriptEngineClass = config.getOrDefault("engine", "my.com.solutionx.simplyscript.nashorn.ScriptEngine");
        Class<ScriptEngineInterface> scriptEngineClass;
        try {
            scriptEngineClass = (Class<ScriptEngineInterface>) Class.forName(ScriptEngineClass);
            Constructor<ScriptEngineInterface> declaredConstructor = scriptEngineClass.getDeclaredConstructor();
            this.engine = declaredConstructor.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ScriptServiceException("Script Engine Class not found: " + e.getMessage(), "E_CannotFindEngineClass");
        } catch (NoSuchMethodException e) {
            throw new ScriptServiceException("Script Engine constructor not found: " + e.getMessage(), "E_CannotFindEngineConstructor");
        } catch (SecurityException e) {
            throw new ScriptServiceException("Error getting Script Engine Constructor: " + e.getMessage(), "E_ErrorGettingEngineConstructor");
        } catch (InstantiationException e) {
            throw new ScriptServiceException("Error Creating Script Engine: " + e.getMessage(), "E_ErrorCreatingEngineConstructor");
        } catch (IllegalAccessException e) {
            throw new ScriptServiceException("Error calling Script Engine Constructor: " + e.getMessage(), "E_ErrorCallingEngineConstructor");
        } catch (IllegalArgumentException e) {
            throw new ScriptServiceException("Invalid Argument sent to Script Engine Constructor: " + e.getMessage(), "E_InvalidArgumentEngineConstructor");
        } catch (InvocationTargetException e) {
            throw e;
        }

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
        mapScriptConfig.put("config", config);
        engine.init(this, mapScriptConfig);

        PoolableScriptContextAllocator allocator = new PoolableScriptContextAllocator(engine);
        PoolBuilder<PoolableScriptContext> poolBuilder = Pool.from(allocator);
        poolBuilder = poolBuilder.setSize(Integer.valueOf(pool_size));
        poolContext = poolBuilder.build();
        
        ScriptContextInterface ctx = engine.getScriptContext();
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
    }

    public ScriptEngineInterface engine() {
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
    
    public Object getService(String name) throws ScriptException, PoolException, InterruptedException {
        return engine.getService(name);
    }
    
    public Pool<PoolableScriptContext> getScriptContextPool() {
        return poolContext;
    }

    public Object action(String action) throws ScriptException, PoolException, InterruptedException {
        return engine.action(action, null);
    }
    
    public String actionReturnString(String action) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        return engine.actionReturnString(action, null);        
    }

    public Object action(String action, Object args) throws ScriptException, PoolException, InterruptedException {
        return engine.action(action, args);        
    }

    public String actionReturnString(String action, Object args) throws ScriptException, PoolException, InterruptedException, JsonProcessingException {
        return engine.actionReturnString(action, args);        
    }

}
