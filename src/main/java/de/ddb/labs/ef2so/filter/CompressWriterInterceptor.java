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
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import jakarta.ws.rs.WebApplicationException;
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
        // If already encoded or no entity, just continue
        LOG.trace("CompressWriterInterceptor invoked");
        if (context.getEntity() == null || context.getHeaders().containsKey(HDR_CONTENT_ENCODING)) {
            context.proceed();
            return;
        }

        // Preferred encoding is determined in CompressRequestFilter and stored as a
        // property
        final Object encObj = context.getProperty(CompressRequestFilter.REQ_PROP_ENCODING);
        final String chosen = (encObj instanceof String) ? (String) encObj : null;

        if (chosen != null) {
            final OutputStream os = context.getOutputStream();
            switch (chosen) {
                case "br":
                    if (BROTLI_AVAILABLE) {
                        context.setOutputStream(new BrotliOutputStream(os));
                        context.getHeaders().add(HDR_CONTENT_ENCODING, "br");
                    }
                    break;
                case "gzip":
                    context.setOutputStream(new GZIPOutputStream(os));
                    context.getHeaders().add(HDR_CONTENT_ENCODING, "gzip");
                    break;
                case "deflate":
                    context.setOutputStream(new DeflaterOutputStream(os));
                    context.getHeaders().add(HDR_CONTENT_ENCODING, "deflate");
                    break;
                default:
                    // no-op
            }

            if (context.getHeaders().containsKey(HDR_CONTENT_ENCODING)) {
                context.getHeaders().remove(HDR_CONTENT_LENGTH);
                context.getHeaders().add(HDR_VARY, HDR_ACCEPT_ENCODING);
            }
        }

        context.proceed();
    }
}
