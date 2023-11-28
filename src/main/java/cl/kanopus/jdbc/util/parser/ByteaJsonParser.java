package cl.kanopus.jdbc.util.parser;

import cl.kanopus.common.util.GsonUtils;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public abstract class ByteaJsonParser {

    private ByteaJsonParser() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> T parse(Class<T> type, String json) {
        return GsonUtils.custom.fromJson(json, type);
    }

}
