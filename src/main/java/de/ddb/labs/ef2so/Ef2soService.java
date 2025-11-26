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
package de.ddb.labs.ef2so;

import de.ddb.labs.ef2so.filter.Compress;
import de.ddb.labs.ef2so.processor.Processor;
import de.ddb.labs.ef2so.processor.ProcessorFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.regex.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Path("/")
public class Ef2soService {

    private static final Logger LOG = LoggerFactory.getLogger(Ef2soService.class);
    private static final String EF_URL = "https://hub.culturegraph.org/entityfacts/";
    private static final String GND_IDN_PATTERN = "(1[012]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X])";
    private final Pattern gndIdnPattern = Pattern.compile(GND_IDN_PATTERN);

    public Ef2soService() {
    }

    /**
     * Root entry point without IDN
     *
     * @param headers HTTP Request Header
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot(@Context HttpHeaders headers) {
        return get(headers, "");
    }

    /**
     * Entry point with IDN
     *
     * @param headers HTTP Request Header
     * @param idn IDN, the number from GND-URI
     * @return
     */
    @GET
    @Compress
    @Path("{idn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpHeaders headers, @PathParam("idn") String idn) {
        try {
            if (idn.isBlank()) {
                throw new InvalidParameterException("No IDN passed");
            }
            if (!gndIdnPattern.matcher(idn).matches()) {
                throw new InvalidParameterException("Invalid IDN passed: '" + idn + "'");
            }
            LOG.info("Execute request for IDN '{}'...", idn);
            final URL url = URI.create(EF_URL + idn).toURL();
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("Accept-Language", "en");

            if (conn.getResponseCode() != 200) {
                final String result = inputStreamToString(conn.getErrorStream(), "UTF-8");
                final String contenType = conn.getContentType();
                final int responseCode = conn.getResponseCode();
                conn.disconnect();
                return Response
                        .status(responseCode)
                        .type(contenType)
                        .entity(result)
                        .build();
            }

            final Processor p = ProcessorFactory.getInstance().getFreeProcessor();
            final String result = p.process(inputStreamToString(conn.getInputStream(), "UTF-8"));

            p.setFree(); // always set the processor free from outside
            conn.disconnect();

            if (result.isEmpty()) {
                return Response
                        .status(501)
                        .entity("{\"Error\":\"Requested resource '" + idn + "' is NOT supported by Schema.org\"}")
                        .build();
            }

            return Response
                    .status(200)
                    .entity(result)
                    .build();

        } catch (InvalidParameterException e) {
            LOG.warn(e.getMessage());
            return Response
                    .status(500)
                    .entity("{\"Error\":\"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response
                    .status(500)
                    .entity("{\"Error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Converts a InputString to a String
     *
     * @param is Input as InputString
     * @param charset Charset ("UTF-8")
     * @return String with content
     * @throws java.io.IOException
     */
    public static String inputStreamToString(final InputStream is, final String charset) throws IOException {
        if (is == null) {
            return "";
        }
        try (is;  BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charset)))) {
            final StringBuilder sb = new StringBuilder();
            int value;
            while ((value = br.read()) != -1) {
                sb.append((char) value);
            }
            return sb.toString();
        }
    }
}
