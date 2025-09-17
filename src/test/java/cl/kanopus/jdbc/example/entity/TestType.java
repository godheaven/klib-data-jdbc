package cl.kanopus.jdbc.example.entity;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.Table;

/**
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@Table(name = "tmp_test_type", keys = {"pk_test_type"})
public class TestType extends Mapping {

    @Column(name = "pk_test_type")
    private long id;

    @Column(name = "name", alias = "tt_name", length = 10)
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
