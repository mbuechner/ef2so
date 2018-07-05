package de.indernetz.ef2so;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.bazaarvoice.jolt.Sortr;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

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
    private final List chainrSpecJSON = JsonUtils.classpathToList("/transform_ef2so.json");
    private final Chainr chainr = Chainr.fromSpec(chainrSpecJSON);

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

            final Object inputJSON = JsonUtils.jsonToObject(conn.getInputStream());
            final Object transformedOutput = Sortr.sortJson(chainr.transform(inputJSON));

            return Response
                    .status(conn.getResponseCode())
                    .entity(JsonUtils.toJsonString(transformedOutput))
                    .build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response
                    .status(500)
                    .entity("{\"Error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            final StringBuffer sb = new StringBuffer();
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
                sb.append(System.getProperty("line.separator"));
            }
            return sb.toString();
        }
    }
}
