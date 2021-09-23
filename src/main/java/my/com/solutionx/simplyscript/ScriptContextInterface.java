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

import java.util.Map;
import javax.script.ScriptException;

/**
 *
 * @author SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;
 */
public interface ScriptContextInterface {

    public void init() throws ScriptException;
    public void setRequest(Map<String, Object> mapReq);
    public void recycle();
    public void cleanup();    

    public Object service(String service, Object ctx) throws ScriptException;
    public Object module(String service, Object ctx);

    public Object app(String key);
    public Object app(String key, Object value);
    public Object cache(String key);
    public void cache(String key, Object value);
    // public Object system(String key);
    // public Object system(String key, Object value);
    public Object req(String key);
    public Object req(String key, Object value);

}
