package dev.mccue.microhttp.reflectivehandler;

import dev.mccue.microhttp.handler.IntoResponse;

import java.lang.reflect.Method;

public interface ResponseCoercer {
    /**
     * Coerce the value returned by a {@link Method} into an {@link IntoResponse}.
     *
     * <p>
     *     If this is not doable, it is expected that an {@link Exception} will be thrown.
     * </p>
     *
     * <p>
     *     If that {@link Exception} is an instance of {@link IntoResponse} then that
     *     can be used to give a specific error to the user.
     * </p>
     * @param method The method that was called. Can be inspected for annotations and declared return type.
     * @param o The object returned from the method.
     * @return An instance of {@link IntoResponse}
     */
    IntoResponse coerce(Method method, Object o) throws Exception;
}
