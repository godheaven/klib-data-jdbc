package cl.kanopus.jdbc.util.extension;

import cl.kanopus.common.data.enums.SortOrder;

public class OrderBy {

    private String column;
    private SortOrder sort;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public SortOrder getSort() {
        return sort;
    }

    public void setSort(SortOrder sort) {
        this.sort = sort;
    }

}
