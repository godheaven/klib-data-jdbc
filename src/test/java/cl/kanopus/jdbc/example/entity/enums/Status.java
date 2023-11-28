
package cl.kanopus.jdbc.example.entity.enums;

import cl.kanopus.common.enums.EnumIdentifiable;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public enum Status implements EnumIdentifiable<String>{
    ERROR,
    SUCCESS;

    @Override
    public String getId() {
        return this.name();
    }

}
