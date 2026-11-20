package de.ddb.labs.ef2so.filter;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CompressionTest {

    @ParameterizedTest
    @CsvSource({
        "'br;q=0.3, GZIP;Q=0.8', gzip",
        "'br;q=0, gzip;q=0.5, *;q=0.9', deflate",
        "'gzip;q=0.5, identity;q=0.8', identity",
        "'gzip;q=0, br;q=broken', identity",
        "'identity;q=0, *;q=0',",
        "'gzip;q=0.8, identity;q=0, *;q=0', gzip",
        "'identity;q=0.5, *;q=0', identity",
        "'br, gzip;q=0.8', br",
        "'', identity",
        ", identity"
    })
    void negotiatesSupportedEncodings(String header, String expected) {
        assertEquals(expected, CompressRequestFilter.selectEncoding(header, List.of("br", "gzip", "deflate")));
    }

    @Test
    void fallsBackToAvailableCompression() {
        assertEquals("gzip", CompressRequestFilter.selectEncoding("br, gzip;q=0.8", List.of("gzip", "deflate")));
    }

    @Test
    void variesResponsesWithoutAWriterAndPreservesOtherVaryValues() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        final ContainerResponseContext response = proxy(ContainerResponseContext.class, (ignored, method, args) -> switch (method.getName()) {
            case "getHeaders" -> headers;
            case "getHeaderString" -> String.join(", ", headers.get(args[0]).stream().map(Object::toString).toList());
            default -> throw new UnsupportedOperationException(method.getName());
        });
        final CompressRequestFilter filter = new CompressRequestFilter();

        filter.filter(null, response);
        filter.filter(null, response);

        assertEquals(List.of(HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.ACCEPT_ENCODING), headers.get(HttpHeaders.VARY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gzip", "deflate", "br"})
    void finishesCompressionAndKeepsContainerStreamOpen(String encoding) throws IOException {
        if ("br".equals(encoding)) {
            assumeTrue(Brotli4jLoader.isAvailable());
        }
        final byte[] entity = "Entity Facts enthält UTF-8-Daten.".getBytes(StandardCharsets.UTF_8);
        final ContainerStream containerStream = new ContainerStream();
        final AtomicReference<OutputStream> stream = new AtomicReference<>(containerStream);
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HttpHeaders.CONTENT_LENGTH, entity.length);
        final WriterInterceptorContext writer = proxy(WriterInterceptorContext.class, (ignored, method, args) -> {
            return switch (method.getName()) {
                case "getProperty" -> encoding;
                case "getEntity" -> entity;
                case "getHeaders" -> headers;
                case "getOutputStream" -> stream.get();
                case "setOutputStream" -> {
                    stream.set((OutputStream) args[0]);
                    yield null;
                }
                case "proceed" -> {
                    stream.get().write(entity);
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });

        new CompressWriterInterceptor().aroundWriteTo(writer);

        assertSame(containerStream, stream.get());
        assertFalse(containerStream.closed);
        assertEquals(encoding, headers.getFirst(HttpHeaders.CONTENT_ENCODING));
        assertNull(headers.getFirst(HttpHeaders.CONTENT_LENGTH));
        final ByteArrayInputStream compressed = new ByteArrayInputStream(containerStream.toByteArray());
        try (InputStream decoded = switch (encoding) {
            case "gzip" -> new GZIPInputStream(compressed);
            case "deflate" -> new InflaterInputStream(compressed);
            case "br" -> new BrotliInputStream(compressed);
            default -> throw new IllegalArgumentException(encoding);
        }) {
            assertArrayEquals(entity, decoded.readAllBytes());
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static final class ContainerStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
