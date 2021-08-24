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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import javax.script.ScriptException;
import stormpot.PoolException;

/**
 *
 * @author SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;
 */
public interface ScriptEngineInterface {
    public void init(ScriptService aThis, Map<String, Object> mapScriptConfig) throws ScriptException, IOException;
    public ScriptContextInterface getScriptContext();
    public Object eval(String script, ScriptContextInterface ctx) throws ScriptException;

    public Object getService(String name)  throws ScriptException, PoolException, InterruptedException;

    public Object action(String action, Object args) throws ScriptException, PoolException, InterruptedException;
    public String actionReturnString(String action, Object args) throws ScriptException, PoolException, InterruptedException, JsonProcessingException;

    public void addClasspath(String path) throws MalformedURLException;
}
