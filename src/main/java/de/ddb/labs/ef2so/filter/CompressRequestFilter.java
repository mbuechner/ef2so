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
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Request filter that determines the desired compression encoding from the
 * client's Accept-Encoding header and stores it as a request property for
 * the writer interceptor to use. Using a request property is more reliable
 * across containers than injecting HttpHeaders into the interceptor.
 */
@Provider
@Compress
@Priority(Priorities.HEADER_DECORATOR)
public class CompressRequestFilter implements ContainerRequestFilter {

    public static final String REQ_PROP_ENCODING = "ef2so.compress.encoding";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String accept = requestContext.getHeaderString("Accept-Encoding");
        if (accept == null || accept.isEmpty()) {
            return;
        }

        // Parse tokens ignoring q-values
        String chosen = null;
        final String[] parts = accept.split("\\s*,\\s*");
        boolean brotliAvailable = BrotliLoader.isBrotliAvailable();
        for (String p : parts) {
            if (p == null || p.isEmpty())
                continue;
            int sc = p.indexOf(';');
            String token = (sc >= 0 ? p.substring(0, sc) : p).trim();
            if (token.isEmpty())
                continue;
            // Choose best supported option in order of client preference
            if ("br".equals(token) && brotliAvailable) {
                chosen = "br";
                break;
            }
            if ("gzip".equals(token) && chosen == null) {
                chosen = "gzip";
            }
            if ("deflate".equals(token) && chosen == null) {
                chosen = "deflate";
            }
        }

        if (chosen != null) {
            requestContext.setProperty(REQ_PROP_ENCODING, chosen);
        }
    }
}
