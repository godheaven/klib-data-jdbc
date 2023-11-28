package cl.kanopus.jdbc.util.extension;

import cl.kanopus.common.enums.EnumIdentifiable;
import cl.kanopus.jdbc.util.SQLQueryDynamic.MatchMode;

/**
 *
 * @author pablo
 */
public enum DataType implements EnumIdentifiable<String> {
    ALPHANUMERIC(
            MatchMode.EQUAL,
            MatchMode.NOT_EQUAL,
            MatchMode.TEXT_CONTAINS,
            MatchMode.TEXT_STARTS_WITH,
            MatchMode.TEXT_ENDS_WITH,
            MatchMode.IN
    ),
    NUMERIC(
            MatchMode.EQUAL,
            MatchMode.LESS_THAN,
            MatchMode.LESS_OR_EQUAL,
            MatchMode.GREATER_THAN,
            MatchMode.GREATER_OR_EQUAL,
            MatchMode.NOT_EQUAL,
            MatchMode.TEXT_CONTAINS,
            MatchMode.TEXT_STARTS_WITH,
            MatchMode.TEXT_ENDS_WITH,
            MatchMode.IN,
            MatchMode.BETWEEN
    ),
    DATE(
            MatchMode.EQUAL,
            MatchMode.LESS_THAN,
            MatchMode.LESS_OR_EQUAL,
            MatchMode.GREATER_THAN,
            MatchMode.GREATER_OR_EQUAL,
            MatchMode.NOT_EQUAL,
            MatchMode.TEXT_CONTAINS,
            MatchMode.TEXT_STARTS_WITH,
            MatchMode.TEXT_ENDS_WITH,
            MatchMode.BETWEEN
    );

    private final MatchMode[] matchModes;

    DataType(MatchMode... matchModes) {
        this.matchModes = matchModes;
    }

    @Override
    public String getId() {
        return this.name();
    }

    public MatchMode[] getMatchModes() {
        return matchModes;
    }

}
