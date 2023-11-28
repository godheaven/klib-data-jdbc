package cl.kanopus.jdbc.util.parser;

import cl.kanopus.common.enums.EnumIdentifiable;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public abstract class EnumParser {

    private EnumParser() {
        throw new IllegalStateException("Utility class");
    }

    public static <T extends Enum<T> & EnumIdentifiable<S>, S> T parse(Class<T> type, S id) {
        for (T t : type.getEnumConstants()) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

}
