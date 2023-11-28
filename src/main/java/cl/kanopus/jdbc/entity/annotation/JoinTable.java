package cl.kanopus.jdbc.entity.annotation;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.enums.JoinOperator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinTable {

    Class<? extends Mapping> table();

    boolean lazy() default false;

    String foreignKey() default "[unassigned]";

    JoinOperator operator() default JoinOperator.INNER_JOIN;
}
