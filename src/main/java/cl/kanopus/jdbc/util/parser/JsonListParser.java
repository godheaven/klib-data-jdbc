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
public abstract class JsonListParser {

    private JsonListParser() {
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
