package cl.kanopus.jdbc.util.extension;

import cl.kanopus.jdbc.util.SQLQueryDynamic.MatchMode;
import java.util.ArrayList;
import java.util.List;

public class GroupCondition {

    private List<Condition> conditions;

    public GroupCondition() {
        this.conditions = new ArrayList<>();
    }

    public GroupCondition(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }
    
    public static class Condition {

        public Condition() {
        }

        public Condition(String column, Object value, DataType dataType, MatchMode matchMode) {
            this.column = column;
            this.value = value;
            this.dataType = dataType;
            this.matchMode = matchMode;
        }

        private String column;
        private Object value;
        private DataType dataType;
        private MatchMode matchMode;
        private List<Condition> conditionsOR;

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public DataType getDataType() {
            return dataType;
        }

        public void setDataType(DataType dataType) {
            this.dataType = dataType;
        }

        public MatchMode getMatchMode() {
            return matchMode;
        }

        public void setMatchMode(MatchMode matchMode) {
            this.matchMode = matchMode;
        }

        public List<Condition> getConditionsOR() {
            return conditionsOR;
        }

        public void setConditionsOR(List<Condition> conditionsOR) {
            this.conditionsOR = conditionsOR;
        }

    }
}
