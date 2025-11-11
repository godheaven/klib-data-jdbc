/*-
 * !--
 * For support and inquiries regarding this library, please contact:
 *   soporte@kanopus.cl
 * 
 * Project website:
 *   https://www.kanopus.cl
 * %%
 * Copyright (C) 2025 Pablo Díaz Saavedra
 * %%
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
 * --!
 */
package cl.kanopus.jdbc.util.parser;

import cl.kanopus.common.util.GsonUtils;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public abstract class ByteaJsonListParser {

    private ByteaJsonListParser() {
        throw new IllegalStateException("Utility class");
    }

    public static <T extends List<T>> T parse(Class<T> type, String json) {
        Type type2 = TypeToken.getParameterized(ArrayList.class, type).getType();
        T result = GsonUtils.custom.fromJson(json, type2);
        if (result == null) {
            result = (T) new ArrayList<T>();
        }
        return result;
    }

}
