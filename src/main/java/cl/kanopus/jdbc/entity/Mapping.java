package cl.kanopus.jdbc.entity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class must be implemented by all Java class that represents a
 * database table, is equivalent to an Entity Object.
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
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
