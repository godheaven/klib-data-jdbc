package cl.kanopus.jdbc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuppressWarnings("all")
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    String name();

    String alias() default "";

    Class parser() default Column.class;

    Class parserResult() default Column.class;

    boolean auditable() default true;

    boolean encrypted() default false;

    boolean serial() default false;

    boolean updatable() default true;

    boolean insertable() default true;

    boolean lazy() default false;

    int length() default 0;

}
