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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

public class ScriptObjectMirrorSerializer extends StdSerializer<ScriptObjectMirror> {

    public ScriptObjectMirrorSerializer() {
        this(null);
    }

    public ScriptObjectMirrorSerializer(Class t) {
        super(t);
    }

    @Override
    public void serialize(ScriptObjectMirror t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        System.out.println(t.getClassName());
        if (t.isArray()) {
            jg.writeStartArray(t.size());
            for (int i=0; i<t.size(); i++) {
                jg.writeObject(t.getSlot(i));
            }
            jg.writeEndArray();
        } else if (t.isFunction()) {
            jg.writeString(t.getDefaultValue(String.class).toString());
        } else {
            jg.writeStartObject();
            for (String key : t.keySet()) {
                jg.writeFieldName(key);
                jg.writeObject(t.get(key));
            }
            jg.writeEndObject();
        }
    }
}