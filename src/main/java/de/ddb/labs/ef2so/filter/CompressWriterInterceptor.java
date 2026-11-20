/*
 * Copyright 2018-2026 Michael Büchner, Deutsche Digitale Bibliothek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.ddb.labs.ef2so.filter;

import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Provider
@Compress
@Priority(3000)
public class CompressWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        final Object encoding = context.getProperty(CompressRequestFilter.REQ_PROP_ENCODING);
        if (!(encoding instanceof String chosen) || context.getEntity() == null
                || context.getHeaders().containsKey(HttpHeaders.CONTENT_ENCODING)) {
            context.proceed();
            return;
        }

        final OutputStream original = context.getOutputStream();
        final OutputStream containerStream = new FilterOutputStream(original) {
            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                out.write(bytes, offset, length);
            }

            @Override
            public void close() {
                // The container owns the original stream.
            }
        };

        context.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, chosen);
        context.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        try (OutputStream compressed = switch (chosen) {
            case "br" -> new BrotliOutputStream(containerStream);
            case "gzip" -> new GZIPOutputStream(containerStream);
            case "deflate" -> new DeflaterOutputStream(containerStream);
            default -> throw new IllegalStateException("Unexpected content coding: " + chosen);
        }) {
            context.setOutputStream(compressed);
            context.proceed();
        } finally {
            context.setOutputStream(original);
        }
    }
}
