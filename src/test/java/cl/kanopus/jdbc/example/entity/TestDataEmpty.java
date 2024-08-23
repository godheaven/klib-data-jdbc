package cl.kanopus.jdbc.example.entity;

import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.Table;

/**
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 * <pre>
 * CREATE TABLE tmp_test_data_empty
 * (
 * pk_test_data_empty integer NOT NULL
 * );
 *
 * </pre>
 */
@Table(name = "tmp_test_data_empty", keys = {"pk_test_data_empty"}, defaultOrderBy = "pk_test_data_empty")
public class TestDataEmpty extends TestViewData {

    @Column(name = "pk_test_data_empty")
    private long id;
}
