/*
 * Copyright 2018 Michael Büchner, Deutsche Digitale Bibliothek.
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
package de.indernetz.ef2so;

import de.indernetz.ef2so.processor.Processor;
import de.indernetz.ef2so.processor.ProcessorFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class Ef2soService {

    private static final Logger LOG = LoggerFactory.getLogger(Ef2soService.class);
    private static final String EF_URL = "http://hub.culturegraph.org/entityfacts/";

    @Context
    private UriInfo context;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoot() {
        return this.get("");
    }

    @GET
    @Path("{idn}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("idn") String idn) {

        try {
            final URL url = new URL(EF_URL + idn);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if (conn.getResponseCode() != 200) {
                return Response
                        .status(conn.getResponseCode())
                        .type(conn.getContentType())
                        .entity(conn.getErrorStream())
                        .build();
            }

            LOG.info("Execute request for IDN " + idn);

            final Processor p = ProcessorFactory.getInstance().getFreeProcessor();
            final String result = p.process(inputStreamToString(conn.getInputStream(), "UTF-8"));
            p.setFree(); // always set the processor free

            if (result.isEmpty()) {
                return Response
                        .status(501)
                        .entity("{\"Error\":\"Requested resource '" + idn + "' NOT supported by Schema.org.\"}")
                        .build();
            }

            return Response
                    .status(conn.getResponseCode())
                    .entity(result)
                    .build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response
                    .status(500)
                    .entity("{\"Error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    private static String inputStreamToString(InputStream is, String charsetName) throws IOException {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName(charsetName)));
            final StringBuilder stringBuffer = new StringBuilder();
            int value;
            while ((value = reader.read()) != -1) {
                stringBuffer.append((char) value);
            }
            return stringBuffer.toString();
        } finally {
            is.close();
        }
    }
}
