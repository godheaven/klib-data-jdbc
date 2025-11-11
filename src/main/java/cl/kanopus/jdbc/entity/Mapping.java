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
package cl.kanopus.jdbc.entity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class must be implemented by all Java class that represents a
 * database table, is equivalent to an Entity Object.
 */
public abstract class Mapping implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mapping.class);

    protected void prePersist() {
    }

    protected void preUpdate() {
    }

    @Override
    public String toString() {

        Class<?> clase = this.getClass();
        Method[] methods = clase.getMethods();
        String result;
        Method method;

        StringBuilder aux = new StringBuilder("Class : [" + this.getClass().getName() + "]\n");
        Object obj;
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().startsWith("get") && !methods[i].getName().equals("getClass")) {
                try {
                    method = methods[i];
                    obj = method.invoke(this, (Object[]) null);
                    if (obj != null) {
                        result = obj.toString();
                    } else {
                        result = null;
                    }
                    aux.append(method.getName().substring(3)).append(" : [").append(result).append("]\n");

                } catch (SecurityException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        return aux.toString();

    }

}
