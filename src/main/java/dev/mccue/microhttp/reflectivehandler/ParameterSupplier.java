package dev.mccue.microhttp.reflectivehandler;

import dev.mccue.microhttp.handler.IntoResponse;
import org.microhttp.Request;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public interface ParameterSupplier {
    /**
     * Supplies a value for a {@link Parameter} in a {@link Method}.
     *
     * <p>
     *     If it is impossible to supply a value for the given parameter, a {@link Exception} should
     *     be thrown.
     * </p>
     * <p>
     *     If that {@link Exception} is an instance of {@link IntoResponse} then that
     *     can be used to give a specific error to the user.
     * </p>
     *
     * @param method The method being called.
     * @param parameter The parameter on that method.
     * @param matcher The regex matcher for the url. Can be used to pull out route params.
     * @param request The request being handled.
     * @return A value for the parameter.
     * @throws Exception If a value cannot be supplied.
     */
    Object provide(
            Method method,
            Parameter parameter,
            Matcher matcher,
            Request request
    ) throws Exception;
}
