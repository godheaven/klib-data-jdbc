package cl.kanopus.jdbc.example.entity;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.Table;

/**
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 * <pre>
 * CREATE TABLE tmp_test_type
 * (
 * pk_test_type integer NOT NULL,
 * name character varying(10) NOT NULL,
 * CONSTRAINT tmp_test_type_pkey PRIMARY KEY (pk_test_type)
 * );
 * ALTER TABLE tmp_test_type OWNER TO user_test;
 *
 * INSERT INTO tmp_test_type(pk_test_type, name) VALUES(1, 'ONE');
 * INSERT INTO tmp_test_type(pk_test_type, name) VALUES(2, 'TWO');
 * </pre>
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
