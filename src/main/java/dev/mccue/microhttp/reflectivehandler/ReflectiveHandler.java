package dev.mccue.microhttp.reflectivehandler;

import dev.mccue.microhttp.handler.Handler;
import dev.mccue.microhttp.handler.IntoResponse;
import dev.mccue.microhttp.handler.RouteHandler;
import org.jspecify.annotations.Nullable;
import org.microhttp.Request;
import org.microhttp.Response;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of {@link Handler} which delegates
 * to methods in the provided object that are annotated with {@link Route}.
 */
public final class ReflectiveHandler implements Handler {
    private final ArrayList<RouteHandler> routeHandlers;

    private ReflectiveHandler(
            ParameterSupplier parameterSupplier,
            ResponseCoercer responseCoercer,
            Object o
    ) {
        this.routeHandlers = new ArrayList<>();
        setupRouteHandlers(parameterSupplier, responseCoercer, o, this.routeHandlers);
    }

    public static ReflectiveHandler of(Object o) {
        return of(null, null, o);
    }

    public static ReflectiveHandler of(ParameterSupplier parameterSupplier, Object o) {
        return of(parameterSupplier, null, o);
    }

    public static ReflectiveHandler of(ResponseCoercer responseCoercer, Object o) {
        return of(null, responseCoercer, o);
    }

    public static ReflectiveHandler of(
            ParameterSupplier parameterSupplier,
            ResponseCoercer responseCoercer,
            Object o
    ) {
        return new ReflectiveHandler(parameterSupplier, responseCoercer, o);
    }

    private static void setupRouteHandlers(ParameterSupplier parameterSupplier, ResponseCoercer responseCoercer, Object o, ArrayList<RouteHandler> routeHandlers) {
        for (var method : o.getClass().getMethods()) {
            List<Route> routeAnnotations = null;
            var routes = method.getAnnotation(Routes.class);
            if (routes == null) {
                var route = method.getAnnotation(Route.class);
                if (route != null) {
                    routeAnnotations = Collections.singletonList(route);
                }
            }
            else {
                routeAnnotations = Arrays.asList(routes.value());
            }

            if (routeAnnotations != null) {
                for (var route : routeAnnotations) {
                    var routeMethod = route.method();
                    var pattern = Pattern.compile(route.pattern());
                    var routeHandler = getRouteHandler(
                            parameterSupplier, responseCoercer, o, method, routeMethod, pattern
                    );
                    routeHandlers.add(routeHandler);
                }
            }
        }
    }

    private static RouteHandler getRouteHandler(
            ParameterSupplier parameterSupplier,
            ResponseCoercer responseCoercer,
            Object o,
            Method method,
            String routeMethod,
            Pattern pattern
    ) {
        Object[] params = new Object[method.getParameterCount()];
        var parameterTypes = method.getParameterTypes();
        var parameters = method.getParameters();

        return RouteHandler.of(routeMethod, pattern, (matcher, request) -> {
            for (int i = 0; i < method.getParameterCount(); i++) {
                var parameterType = parameterTypes[i];
                if (parameterType == Request.class) {
                    params[i] = request;
                }
                else if (parameterType == Matcher.class) {
                    params[i] = matcher;
                }
                else if (parameterSupplier != null) {
                    params[i] = parameterSupplier.provide(method, parameters[i], matcher, request);
                }
                else {
                    throw new RuntimeException(
                            "Do not know how to provide [" + parameters[i] + "] to [" + method + "]"
                    );
                }
            }

            var result = method.invoke(o, params);
            if (result instanceof IntoResponse intoResponse) {
                return intoResponse;
            }
            else if (result instanceof Response response) {
                return () -> response;
            }
            else if (result == null) {
                return null;
            }
            else if (responseCoercer != null) {
                return responseCoercer.coerce(method, result);
            }
            else {
                throw new RuntimeException(
                        "Do not know how to coerce [" +
                        result.getClass().getName() +
                        "] into a response"
                );
            }

        });
    }

    @Override
    public @Nullable IntoResponse handle(Request request) throws Exception {
        try {
            for (var handler : routeHandlers) {
                var response = handler.handle(request);
                if (response != null) {
                    return response;
                }
            }
        } catch (Exception e) {
            if (e instanceof IntoResponse errorResponse) {
                return errorResponse;
            }
            throw e;
        }

        return null;
    }
}

