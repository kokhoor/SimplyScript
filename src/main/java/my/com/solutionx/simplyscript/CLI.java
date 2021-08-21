/*
 * Copyright 2021 SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Scanner;
import javax.script.ScriptException;
import stormpot.PoolException;

/**
 *
 * @author SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;
 */
public class CLI {
    public static void main(String argv[]) throws IOException, ScriptException, FileNotFoundException, PoolException, InterruptedException, ScriptServiceException, InvocationTargetException {
        if (System.in.available() > 0 && argv.length == 1) {
            System.out.println("Calling action: " + argv[0]);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> map = mapper.readValue(System.in, Map.class);
            System.out.println(map);
        } else {
            System.out.printf("Has data: %d%n", System.in.available());

            for (int i=0; i<argv.length; i++) {
                System.out.printf("Argument %d%n", i);
                System.out.println(argv[i]);
            }
        }
/*
        String ini_filename = System.getProperty("config", "config.ini");
        if (ini_filename == null || ini_filename.length() == 0) {
            ini_filename = "config.ini";
        }
        Wini ini = new Wini(new File(ini_filename));
        Profile.Section iniMain = ini.get("main");
        ScriptService engine = new ScriptService();
        engine.init(iniMain);
*/        
    }
}
