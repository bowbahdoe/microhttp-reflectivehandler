package dev.mccue.microhttp.reflectivehandler;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Routes.class)
public @interface Route {
    String method();

    String pattern();
}
