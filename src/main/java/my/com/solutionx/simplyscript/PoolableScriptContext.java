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

import javax.script.ScriptException;
import stormpot.BasePoolable;
import stormpot.Slot;

public class PoolableScriptContext extends BasePoolable {
    ScriptLocalObject ctx = null;
    ScriptEngine global = null;

    PoolableScriptContext(ScriptEngine global, Slot slot) throws ScriptException {
        super(slot);
        this.global = global;
        ctx = new ScriptLocalObject(global);
        ctx.init();
    }

    @Override
    public void release() {
        ctx.recycle();
        super.release();
    }
    
    public ScriptLocalObject getScriptContext() {
        return ctx;
    }
    
    public Object eval(String script) throws ScriptException {
        return global.engine().eval(script, ctx);
    }

    void deallocate() {
        ctx.cleanup();
        ctx = null;
    }
}