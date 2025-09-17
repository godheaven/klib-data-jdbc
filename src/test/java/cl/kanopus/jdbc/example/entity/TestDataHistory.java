package cl.kanopus.jdbc.example.entity;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.Table;

/**
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
@Table(name = "tmp_test_data_history", sequence = "tmp_test_data_history_pk_test_data_history_seq", keys = {"pk_test_data_history"})
public class TestDataHistory extends Mapping {

    @Column(name = "fk_test_data", updatable = false)
    private long testDataId;

    @Column(name = "info", auditable = false)
    private String info;

    public long getTestDataId() {
        return testDataId;
    }

    public void setTestDataId(long testDataId) {
        this.testDataId = testDataId;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

}
