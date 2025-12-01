/*
 * Copyright 2018-2025 Michael Büchner, Deutsche Digitale Bibliothek.
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

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Provider
@Compress
@Priority(3000)
public class CompressWriterInterceptor implements WriterInterceptor {

    @Context
    private HttpHeaders headers;
    private static final Logger LOG = LoggerFactory.getLogger(CompressWriterInterceptor.class);
    private static final String HDR_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String HDR_CONTENT_ENCODING = "Content-Encoding";
    private static final String HDR_CONTENT_LENGTH = "Content-Length";
    private static final String HDR_VARY = "Vary";
    private static final boolean BROTLI_AVAILABLE = BrotliLoader.isBrotliAvailable();

    public CompressWriterInterceptor() {
        // Trigger static init to detect native availability early
        // (result cached in BROTLI_AVAILABLE)
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {

        try {
            // Skip if already encoded or no entity
            if (context.getHeaders().containsKey(HDR_CONTENT_ENCODING)) {
                context.proceed();
                return;
            }

            final String acceptEncoding = headers.getHeaderString(HDR_ACCEPT_ENCODING);
            if (acceptEncoding != null && !acceptEncoding.isEmpty() && context.getEntity() != null) {
                // Normalize tokens, ignore q-values (e.g., "gzip;q=1.0")
                final String[] rawTokens = acceptEncoding.split("\\s*,\\s*");
                final List<String> tokens = new ArrayList<>(rawTokens.length);
                for (String t : rawTokens) {
                    if (t == null || t.isEmpty()) continue;
                    final int scIdx = t.indexOf(';');
                    final String norm = (scIdx >= 0 ? t.substring(0, scIdx) : t).trim();
                    if (!norm.isEmpty()) tokens.add(norm);
                }

                final OutputStream outputStream = context.getOutputStream();

                if (BROTLI_AVAILABLE && tokens.contains("br")) {
                    context.setOutputStream(new BrotliOutputStream(outputStream));
                    context.getHeaders().add(HDR_CONTENT_ENCODING, "br");
                } else if (tokens.contains("gzip")) {
                    context.setOutputStream(new GZIPOutputStream(outputStream));
                    context.getHeaders().add(HDR_CONTENT_ENCODING, "gzip");
                } else if (tokens.contains("deflate")) {
                    context.setOutputStream(new DeflaterOutputStream(outputStream));
                    context.getHeaders().add(HDR_CONTENT_ENCODING, "deflate");
                }

                // If we set an encoding, ensure proper cache negotiation and length handling
                if (context.getHeaders().containsKey(HDR_CONTENT_ENCODING)) {
                    // Remove Content-Length (compressed size is unknown ahead of time)
                    context.getHeaders().remove(HDR_CONTENT_LENGTH);
                    // Add Vary header so caches differentiate by Accept-Encoding
                    context.getHeaders().add(HDR_VARY, HDR_ACCEPT_ENCODING);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            context.proceed();
        }
    }
}
