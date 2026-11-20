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

import com.aayushatharva.brotli4j.Brotli4jLoader;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Negotiates the response coding for the writer interceptor and marks all
 * responses from compression-enabled resources as varying by Accept-Encoding.
 */
@Provider
@Compress
@Priority(Priorities.HEADER_DECORATOR)
public class CompressRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String REQ_PROP_ENCODING = "ef2so.compress.encoding";
    private static final String IDENTITY = "identity";
    private static final Pattern QUALITY = Pattern.compile("0(?:\\.[0-9]{0,3})?|1(?:\\.0{0,3})?");
    private static final List<String> ENCODINGS = Brotli4jLoader.isAvailable()
            ? List.of("br", "gzip", "deflate") : List.of("gzip", "deflate");

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String chosen = selectEncoding(requestContext.getHeaderString(HttpHeaders.ACCEPT_ENCODING), ENCODINGS);
        if (chosen == null) {
            requestContext.setProperty(RequestLoggingFilter.FAILURE_REASON, "encoding_not_acceptable");
            requestContext.abortWith(Response.status(Response.Status.NOT_ACCEPTABLE)
                    .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING)
                    .build());
        } else if (!IDENTITY.equals(chosen)) {
            requestContext.setProperty(REQ_PROP_ENCODING, chosen);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        final String vary = responseContext.getHeaderString(HttpHeaders.VARY);
        if (vary == null || Arrays.stream(vary.split(","))
                .map(String::trim)
                .noneMatch(value -> "*".equals(value) || HttpHeaders.ACCEPT_ENCODING.equalsIgnoreCase(value))) {
            responseContext.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
        }
    }

    static String selectEncoding(String accept, List<String> encodings) {
        if (accept == null || accept.isBlank()) {
            return IDENTITY;
        }

        final Map<String, Double> qualities = new HashMap<>();
        for (String entry : accept.split(",")) {
            final String[] parts = entry.split(";", 2);
            final String encoding = parts[0].trim().toLowerCase(Locale.ROOT);
            if (!encoding.isEmpty()) {
                qualities.merge(encoding, parts.length == 1 ? 1.0 : parseQuality(parts[1]), Math::min);
            }
        }

        final double wildcard = qualities.getOrDefault("*", 0.0);
        String chosen = null;
        double bestQuality = 0;
        for (String encoding : encodings) {
            final double quality = qualities.getOrDefault(encoding, wildcard);
            if (quality > bestQuality) {
                chosen = encoding;
                bestQuality = quality;
            }
        }

        final Double identity = qualities.get(IDENTITY);
        if (identity != null) {
            return identity > bestQuality ? IDENTITY : chosen;
        }
        if (chosen == null && (!qualities.containsKey("*") || wildcard > 0)) {
            return IDENTITY;
        }
        return chosen;
    }

    private static double parseQuality(String parameter) {
        final String[] parts = parameter.split("=", 2);
        if (parts.length == 2 && "q".equalsIgnoreCase(parts[0].trim())) {
            final String value = parts[1].trim();
            if (QUALITY.matcher(value).matches()) {
                return Double.parseDouble(value);
            }
        }
        return 0;
    }
}
