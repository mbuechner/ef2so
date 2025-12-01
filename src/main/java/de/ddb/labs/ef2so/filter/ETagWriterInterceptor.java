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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

/**
 * WriterInterceptor computing a fast ETag over the response payload.
 * Uses streaming xxHash64 (very fast) and hashes the final representation
 * (post-compression) by running before the compression interceptor.
 */
@Provider
@ETag
@Priority(2000)
public class ETagWriterInterceptor implements WriterInterceptor {

    @Context
    private HttpHeaders headers;

    private static final Logger LOG = LoggerFactory.getLogger(ETagWriterInterceptor.class);
    private static final String HDR_ETAG = "ETag";
    private static final XXHashFactory XX = XXHashFactory.fastestInstance();
    private static final long SEED = 0L;

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        // Don't override an explicitly set ETag or skip empty entities
        if (context.getEntity() == null || context.getHeaders().containsKey(HDR_ETAG)) {
            context.proceed();
            return;
        }

        final OutputStream original = context.getOutputStream();
        final StreamingXXHash64 hasher = XX.newStreamingHash64(SEED);
        final OutputStream hashing = new HashingOutputStream(original, hasher);
        context.setOutputStream(hashing);

        try {
            context.proceed();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            long value = hasher.getValue();
            String hex = toFixedHex(value);
            context.getHeaders().putSingle(HDR_ETAG, '"' + hex + '"');
        }
    }

    private static String toFixedHex(long v) {
        // 16 hex chars, zero-padded, lower-case
        return String.format(Locale.ROOT, "%016x", v);
    }

    private static final class HashingOutputStream extends OutputStream {
        private final OutputStream delegate;
        private final StreamingXXHash64 hasher;

        private HashingOutputStream(OutputStream delegate, StreamingXXHash64 hasher) {
            this.delegate = delegate;
            this.hasher = hasher;
        }

        @Override
        public void write(int b) throws IOException {
            byte[] one = {(byte) b};
            hasher.update(one, 0, 1);
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len > 0) {
                hasher.update(b, off, len);
            }
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
