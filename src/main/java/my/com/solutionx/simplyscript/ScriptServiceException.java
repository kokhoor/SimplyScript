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

/**
 *
 * @author SolutionX Software Sdn. Bhd. &lt;info@solutionx.com.my&gt;
 */
public class ScriptServiceException extends Exception {
    public String code = null;
    
    public ScriptServiceException(String errorMessage) {
        super(errorMessage);
    }

    public ScriptServiceException(String errorMessage, String errorCode) {
        super(errorMessage);
        code = errorCode;
    }

    public String getCode() {
        return code;
    }
}
