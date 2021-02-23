/*
 * Copyright 2018-2021 Michael Büchner, Deutsche Digitale Bibliothek.
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

import de.ddb.labs.ef2so.processor.Processor;
import de.ddb.labs.ef2so.processor.ProcessorFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.regex.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Path("/")
public class Ef2soService {

    private static final Logger LOG = LoggerFactory.getLogger(Ef2soService.class);
    private static final String EF_URL = "http://hub.culturegraph.org/entityfacts/";
    private static final String GND_IDN_PATTERN = "(1[012]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X])";
    private final Pattern gndIdnPattern = Pattern.compile(GND_IDN_PATTERN);

    // @Context
    // private UriInfo context;
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
    @Path("{idn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpHeaders headers, @PathParam("idn") String idn) {
        try {

            if (!gndIdnPattern.matcher(idn).matches()) {
                throw new InvalidParameterException("Invalid IDN given.");
            }
            LOG.info("Execute request for IDN '{}'...", idn);
            final URL url = new URL(EF_URL + idn);
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
                        .entity("{\"Error\":\"Requested resource '" + idn + "' NOT supported by Schema.org.\"}")
                        .build();
            }

            return Response
                    .status(200)
                    .entity(result)
                    .build();

        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
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
     * @throws IOException
     */
    public static String inputStreamToString(final InputStream is, final String charset) throws IOException {
        if (is != null) {
            try {
                final BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName(charset)));
                final StringBuilder sb = new StringBuilder();
                int value;
                while ((value = br.read()) != -1) {
                    sb.append((char) value);
                }
                return sb.toString();
            } finally {
                is.close();
            }
        }
        return "";
    }
}
