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
package de.ddb.labs.ef2so;

import de.ddb.labs.ef2so.filter.Compress;
import de.ddb.labs.ef2so.processor.ProcessorFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.regex.Pattern;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.metafacture.framework.MetafactureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.io.JsonStringEncoder;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Path("/")
public class Ef2soService {

    private static final Logger LOG = LoggerFactory.getLogger(Ef2soService.class);
    private static final String EF_URL = "https://hub.culturegraph.org/entityfacts/";
    private static final Pattern GND_IDN_PATTERN =
            Pattern.compile("(1[012]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X])");
    private static final XXHash64 HASH = XXHashFactory.safeInstance().hash64();
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int CACHE_MAX_AGE_SECONDS = 86_400;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot() {
        return error(Response.Status.BAD_REQUEST, "No IDN passed");
    }

    /**
     * @param request the current request, used to evaluate cache preconditions
     * @param idn the number from a GND URI
     * @return the converted record or an error
     */
    @GET
    @Compress
    @Path("{idn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context Request request, @PathParam("idn") String idn) {
        if (!GND_IDN_PATTERN.matcher(idn).matches()) {
            return error(Response.Status.BAD_REQUEST, "Invalid IDN passed");
        }

        try {
            LOG.debug("Fetching Entity Facts for IDN {}", idn);
            final HttpURLConnection conn = (HttpURLConnection) URI.create(EF_URL + idn).toURL().openConnection();
            final String input;
            try {
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("Accept-Language", "en");

                final int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    return upstreamResponse(conn, status);
                }
                input = readUtf8(conn.getInputStream());
            } finally {
                conn.disconnect();
            }

            final var processed = ProcessorFactory.getInstance().tryProcess(input);
            if (processed.isEmpty()) {
                return error(Response.Status.SERVICE_UNAVAILABLE, "No processor available");
            }
            final String result = processed.get();
            if (result.isEmpty()) {
                return error(Response.Status.NOT_IMPLEMENTED,
                        "Requested resource '" + idn + "' is not supported by Schema.org");
            }

            return cacheableResponse(request, result);
        } catch (SocketTimeoutException e) {
            LOG.warn("Entity Facts request timed out for IDN {}", idn, e);
            return error(Response.Status.GATEWAY_TIMEOUT, "Entity Facts request timed out");
        } catch (IOException | MetafactureException e) {
            LOG.warn("Could not read or process Entity Facts for IDN {}", idn, e);
            return error(Response.Status.BAD_GATEWAY, "Could not read or process Entity Facts");
        } catch (RuntimeException e) {
            LOG.error("Could not convert Entity Facts for IDN {}", idn, e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Could not convert Entity Facts");
        }
    }

    private static Response cacheableResponse(Request request, String result) {
        final byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
        final long hash = HASH.hash(bytes, 0, bytes.length, 0L);
        // The same weak validator covers identity, gzip and Brotli.
        final EntityTag tag = new EntityTag(HexFormat.of().toHexDigits(hash), true);
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(CACHE_MAX_AGE_SECONDS);
        final Response.ResponseBuilder preconditions = request.evaluatePreconditions(tag);
        final Response.ResponseBuilder response = preconditions == null
                ? Response.ok(result, MediaType.APPLICATION_JSON) : preconditions;
        return response.cacheControl(cacheControl).tag(tag).build();
    }

    private static Response upstreamResponse(HttpURLConnection conn, int status) throws IOException {
        final String body = readUtf8(conn.getErrorStream());
        final Response.ResponseBuilder response = Response.status(status);
        if (!body.isEmpty()) {
            response.type(conn.getContentType()).entity(body);
        }
        return response.build();
    }

    private static String readUtf8(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Response error(Response.Status status, String message) {
        final StringBuilder body = new StringBuilder("{\"Error\":\"");
        JsonStringEncoder.getInstance().quoteAsString(message, body);
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(body.append("\"}").toString())
                .build();
    }
}
