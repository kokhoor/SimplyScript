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

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import my.com.solutionx.simplyscript.ScriptContextInterface;
import my.com.solutionx.simplyscript.ScriptService;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.internal.runtime.Undefined;

/**
 *
 * @author kokhoor
 */
public class NashornScriptContext extends SimpleScriptContext implements ScriptContextInterface {
    WeakReference<ScriptEngine> global;
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> externalReq = null;

    public NashornScriptContext(ScriptEngine global) {
        super();
        this.global = new WeakReference<>(global);
    }
    
    public void init() throws ScriptException {
        global.get().initScript().eval(this);
    }

    public ScriptObjectMirror ctxConstructor() {
        return global.get().ctxConstructor();
    }

    public Object app(String key) {
        return global.get().app(key);
    }
    
    public Object app(String key, Object value) {
        return global.get().app(key, value);
    }

    public Object cache(String key) {
        return global.get().cache(key);
    }
    
    public void cache(String key, Object value) {
        global.get().cache(key, value);
    }

    public Object system(String key) {
        return global.get().system(key);
    }
    
    public Object system(String key, Object value) {
        return global.get().system(key, value);
    }

    public Object module(String key, Object ctx) {
        return global.get().modules().get(key, (String k) -> {
            ScriptObjectMirror ctxObject = global.get().ctxConstructor();
            return ctxObject.callMember("moduleSetup", key, global.get().system(), ctx);
        });
    }

    public boolean isPrivileged(String uniqueid) {
        ScriptService scriptService = global.get().scriptService.get();
        return scriptService.isPrivilegedService(uniqueid);
    }

    public Object service(String key, Object ctx) throws ScriptException {
        ScriptObjectMirror obj = (ScriptObjectMirror)global.get().service(key);
        if (obj == null) {
            ScriptObjectMirror ctxObject = global.get().ctxConstructor();
            String uuid = UUID.randomUUID().toString();
            ScriptService scriptService = global.get().scriptService.get();
            Map<String, Object> mapScriptConfig = scriptService.getScriptConfig();
            Map<String, Object> mapServiceConfig = (Map<String, Object>) mapScriptConfig.get("service");
            List lstPrivileged = (List)mapServiceConfig.get("privilegedServices");
            if (lstPrivileged != null && lstPrivileged.contains(key)) {
                scriptService.addPrivilegedService(uuid);
            }

            Object ret = ctxObject.callMember("serviceSetup", key, global.get().system(), uuid, ctx);
            if (ret == null || ret.getClass() == Undefined.class)
                throw new RuntimeException("Error instantiating service: " + key);
            obj = (ScriptObjectMirror) ret;
            global.get().service(key, obj);
        }
        return obj;
    }

    public Object req(String key) {
        if (externalReq != null)
            return externalReq.get(key);
        return request.get(key);
    }

    public Object req(String key, Object value) {
        if (externalReq != null)
            return externalReq.put(key, value);
        return request.put(key, value);
    }

    public void addClasspath(String path) throws MalformedURLException {
        global.get().addClasspath(path);
    }

    public void recycle() {
        request.clear();
        externalReq = null;
    }

    public void cleanup() {
        request.clear();
        request = null;
        externalReq = null;
        global.clear();
        global = null;
    }

    @Override
    public void setRequest(Map<String, Object> mapReq) {
        externalReq = mapReq;
    }
}