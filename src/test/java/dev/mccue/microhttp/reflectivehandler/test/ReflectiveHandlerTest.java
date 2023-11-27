package dev.mccue.microhttp.reflectivehandler.test;

import dev.mccue.microhttp.handler.IntoResponse;
import dev.mccue.microhttp.reflectivehandler.ParameterSupplier;
import dev.mccue.microhttp.reflectivehandler.ReflectiveHandler;
import dev.mccue.microhttp.reflectivehandler.ResponseCoercer;
import dev.mccue.microhttp.reflectivehandler.Route;
import org.junit.jupiter.api.Test;
import org.microhttp.Request;
import org.microhttp.Response;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

public class ReflectiveHandlerTest {
    public record EchoRoot(Response r) {
        @Route(method = "GET", pattern = "/")
        public IntoResponse root() {
            return () -> r;
        }
    }

    @Test
    public void testBasicHandler() throws Exception{
        var r = new Response(200, "OK", List.of(), new byte[] { 1, 2, 3 });
        var handler = ReflectiveHandler.of(new EchoRoot(r));

        assertSame(
                r,
                handler
                        .handle(new Request("GET", "/", "", List.of(), new byte[0]))
                        .intoResponse()
        );

        assertNull(
                handler
                        .handle(new Request("POST", "/", "", List.of(), new byte[0]))
        );

        assertNull(
                handler
                        .handle(new Request("GET", "/abc", "", List.of(), new byte[0]))
        );
    }

    public record EchoPathBack() {
        @Route(method = "GET", pattern = "/(?<path>.+)")
        public IntoResponse echo(Matcher matcher) {
            return () -> new Response(200, "OK", List.of(), matcher.group("path")
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testEchoPath() throws Exception {
        var handler = ReflectiveHandler.of(new EchoPathBack());
        assertEquals(
                "apple",
                new String(
                        handler.handle(new Request("GET", "/apple", "", List.of(), new byte[0]))
                                .intoResponse()
                                .body(),
                        StandardCharsets.UTF_8
                )
        );
    }

    public record EchoURIBack() {
        @Route(method = "GET", pattern = "/(?<path>.+)")
        public IntoResponse echo(Request request) {
            return () -> new Response(200, "OK", List.of(), request.uri()
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testEchoURI() throws Exception {
        var handler = ReflectiveHandler.of(new EchoURIBack());
        assertEquals(
                "/apple/orange",
                new String(
                        handler.handle(new Request("GET", "/apple/orange", "", List.of(), new byte[0]))
                                .intoResponse()
                                .body(),
                        StandardCharsets.UTF_8
                )
        );
    }

    public record PostRequest() {
        @Route(method = "POST", pattern = "/")
        public IntoResponse handle(Request request) {
            return () -> new Response(200, "OK", List.of(), "Hello"
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testPostRequest() throws Exception {
        var handler = ReflectiveHandler.of(new PostRequest());
        assertEquals(
                "Hello",
                new String(
                        handler.handle(new Request("POST", "/", "", List.of(), new byte[0]))
                                .intoResponse()
                                .body(),
                        StandardCharsets.UTF_8
                )
        );
        assertNull( handler.handle(new Request("GET", "/", "", List.of(), new byte[0])));
        assertNull( handler.handle(new Request("POST", "/a/b/c", "", List.of(), new byte[0])));
    }

    public record MultipleRoutesSameMethod() {
        @Route(method = "GET", pattern = "/greet")
        @Route(method = "POST", pattern = "/greet")
        public IntoResponse handle(Request request) {
            if (request.method().equalsIgnoreCase("POST")) {
                return () -> new Response(200, "OK", List.of(), "Hello"
                        .getBytes(StandardCharsets.UTF_8));
            }
            else {
                return () -> new Response(200, "OK", List.of(), "World"
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    public void testMultipleRoutes() throws Exception {
        var handler = ReflectiveHandler.of(new MultipleRoutesSameMethod());
        var getResponse = handler.handle(new Request("GET", "/greet", "", List.of(), new byte[0]));
        var postResponse = handler.handle(new Request("POST", "/greet", "", List.of(), new byte[0]));
        assertEquals("World", new String(getResponse.intoResponse().body(), StandardCharsets.UTF_8));
        assertEquals("Hello", new String(postResponse.intoResponse().body(), StandardCharsets.UTF_8));
        assertNull(handler.handle(new Request("GET", "/", "", List.of(), new byte[0])));
        assertNull(handler.handle(new Request("POST", "/", "", List.of(), new byte[0])));
        assertNull(handler.handle(new Request("GET", "/other", "", List.of(), new byte[0])));
        assertNull(handler.handle(new Request("POST", "/other", "", List.of(), new byte[0])));
    }

    public record WantsUnProvidableArg() {
        @Route(method = "GET", pattern = "/")
        public IntoResponse handle(Long l) {
            return () -> null;
        }
    }

    @Test
    public void testWantsUnProvidableArg() throws Exception {
        var handler = ReflectiveHandler.of(new WantsUnProvidableArg());
        assertThrows(Exception.class, () -> handler.handle(new Request("GET", "/", "", List.of(), new byte[0])));
    }

    public record WantsCustomProvidedArg() {
        @Route(method = "GET", pattern = "/")
        public IntoResponse handle(Long l) {
            return () -> new Response(200, "OK", List.of(), String.valueOf(l).getBytes());
        }
    }

    @Test
    public void testWantsCustomProvidedArg() throws Exception {
        ParameterSupplier parameterSupplier = (
                Method method,
                Parameter parameter,
                Matcher matcher,
                Request request
        ) -> {
            if (parameter.getType() == Long.class) {
                return 8L;
            }
            else {
                throw new RuntimeException("Unhandled");
            }
        };
        var handler = ReflectiveHandler.of(parameterSupplier, new WantsCustomProvidedArg());
        assertEquals(
                "8",
                new String(
                        handler.handle(new Request("GET", "/", "", List.of(), new byte[0]))
                            .intoResponse()
                            .body(),
                        StandardCharsets.UTF_8
                )
        );
    }

    public record ReturnsString() {
        @Route(method = "GET", pattern = "/")
        public String get() {
            return "Hello, world";
        }
    }

    @Test
    public void testWithoutReturnTypeCoercer() {
        var handler = ReflectiveHandler.of(new ReturnsString());
        assertThrows(
                Exception.class,
                () -> handler.handle(new Request("GET", "/", "", List.of(), new byte[0]))
        );

    }

    @Test
    public void testWithReturnTypeCoercer() throws Exception {
        ResponseCoercer coercer = (method, o) -> {
            if (o instanceof String s) {
                return () -> new Response(200, "OK", List.of(), s.getBytes());
            }
            else {
                throw new RuntimeException("Unhandled " + o.getClass());
            }
        };

        var handler = ReflectiveHandler.of(coercer, new ReturnsString());
        assertEquals(
                "Hello, world",
                new String(
                        handler.handle(new Request("GET", "/", "", List.of(), new byte[0]))
                                .intoResponse()
                                .body(),
                        StandardCharsets.UTF_8
                )
        );
    }
}
