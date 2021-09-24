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
package my.com.solutionx.simplyscript.graal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.graalvm.polyglot.Value;

public class ValueSerializer extends StdSerializer<Value> {

    public ValueSerializer() {
        this(null);
    }

    public ValueSerializer(Class t) {
        super(t);
    }

    @Override
    public void serialize(Value t, JsonGenerator jg, SerializerProvider sp) throws IOException {
        if (t.hasArrayElements()) {
            long arraySize = t.getArraySize();
            jg.writeStartArray(arraySize);
            for (int i=0; i<arraySize; i++) {
                jg.writeObject(t.getArrayElement(i));
            }
            jg.writeEndArray();
        } else if (t.isHostObject()) {
            jg.writeObject(t.asHostObject());
        } else if (t.hasMembers()) {
            jg.writeStartObject();
            for (String key : t.getMemberKeys()) {
                jg.writeFieldName(key);
                jg.writeObject(t.getMember(key));
            }
            jg.writeEndObject();
        } else if (t.isNumber()) {
            if (t.fitsInLong())
                jg.writeNumber(t.asLong());
            else if (t.fitsInFloat())
                jg.writeNumber(t.asFloat());
            else
                jg.writeNumber(t.asDouble());
        } else if (t.isBoolean()) {
            jg.writeBoolean(t.asBoolean());
        } else if (t.isNull()) {
            jg.writeNull();
        } else {
            jg.writeString(t.asString());
        }
    }
}