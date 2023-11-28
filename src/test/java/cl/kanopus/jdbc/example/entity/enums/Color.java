package cl.kanopus.jdbc.example.entity.enums;

import cl.kanopus.common.enums.EnumIdentifiable;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public enum Color implements EnumIdentifiable<Integer> {

    RED(1),
    BLACK(2);

    private int id;

    Color(int id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

}
