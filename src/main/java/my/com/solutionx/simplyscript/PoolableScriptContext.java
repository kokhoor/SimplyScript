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
package my.com.solutionx.simplyscript;

import javax.script.ScriptException;
import stormpot.BasePoolable;
import stormpot.Slot;

public class PoolableScriptContext extends BasePoolable {
    ScriptContextInterface ctx = null;
    ScriptEngineInterface global = null;

    PoolableScriptContext(ScriptEngineInterface global, Slot slot) throws ScriptException {
        super(slot);
        this.global = global;
        ctx = global.getScriptContext();
        ctx.init();
    }

    @Override
    public void release() {
        ctx.recycle();
        super.release();
    }
    
    public ScriptContextInterface getScriptContext() {
        return ctx;
    }
    
    public Object eval(String script) throws ScriptException {
        return global.eval(script, ctx);
    }

    void deallocate() {
        ctx.cleanup();
        ctx = null;
    }
}