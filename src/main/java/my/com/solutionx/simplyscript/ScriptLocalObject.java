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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.internal.runtime.Undefined;

/**
 *
 * @author kokhoor
 */
public class ScriptLocalObject extends SimpleScriptContext {
    WeakReference<ScriptEngine> global;
    Map<String, Object> request = new HashMap<>();

    public ScriptLocalObject(ScriptEngine global) throws ScriptException {
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

    public Object module(String key) {
        return global.get().modules.get(key, (String k) -> {
            ScriptObjectMirror ctxObject = global.get().ctxConstructor();
            return ctxObject.callMember("moduleSetup", key, global.get().system);
        });
    }

    public Object service(String key) throws ScriptException {
        ScriptObjectMirror obj = (ScriptObjectMirror)global.get().service(key);
        if (obj == null) {
            ScriptObjectMirror ctxObject = global.get().ctxConstructor();
            Object ret = ctxObject.callMember("serviceSetup", key, global.get().system);
            if (ret == null || ret.getClass() == Undefined.class)
                throw new RuntimeException("Service cannot be setup: " + key);
            obj = (ScriptObjectMirror) ret;
            global.get().service(key, obj);
        }
        return obj;
    }

    public Object req(String key) {
        return request.get(key);
    }

    public Object req(String key, Object value) {
        return request.put(key, value);
    }

    public void recycle() {
        request.clear();
    }

    public void cleanup() {
        request.clear();
        request = null;
        global.clear();
        global = null;
    }
}