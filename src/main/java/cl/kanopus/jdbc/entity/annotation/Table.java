package cl.kanopus.jdbc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    String name();

    String sequence() default "[unassigned]";

    String[] keys() default "[unassigned]";

    boolean readonly() default false;

    String defaultOrderBy() default "[unassigned]";

}
