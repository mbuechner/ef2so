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
package de.ddb.labs.ef2so.playground;

import de.ddb.labs.ef2so.metafacture.JsonDecoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.metafacture.json.JsonEncoder;
import org.metafacture.metamorph.Filter;
import org.metafacture.metamorph.Metamorph;
import org.metafacture.strings.StringConcatenator;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
public class MetamorphMain {

    private static final String MORPH_FILTER_SCRIPT = "ef2so_filter.xml";
    private static final String MORPH_TRANS_SCRIPT = "ef2so_transformation.xml";

    public static void main(String[] args) throws MalformedURLException, IOException {
        // final URL url = new URL("http://hub.culturegraph.org/entityfacts/133070557"); // Familie
        // final URL url = new URL("http://hub.culturegraph.org/entityfacts/9776-7");
        final URL url = new URL("http://hub.culturegraph.org/entityfacts/118540238"); // Goethe
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept-Language", "en");

        final Filter f = new Filter(MORPH_FILTER_SCRIPT);
        final Metamorph trans = new Metamorph(MORPH_TRANS_SCRIPT);
        final JsonEncoder jsonEncoder = new JsonEncoder();
        // final StreamLogger logger = new StreamLogger();
        jsonEncoder.setPrettyPrinting(true);

        final JsonDecoder myjsonDecoder = new JsonDecoder();
        final StringConcatenator stringConcatenator = new StringConcatenator();

        myjsonDecoder
                .setReceiver(f)
                // .setReceiver(logger)
                .setReceiver(trans)
                .setReceiver(jsonEncoder)
                .setReceiver(stringConcatenator);

        myjsonDecoder.process(inputStreamToString(conn.getInputStream(), "UTF-8"));

        final String result = stringConcatenator.getString();
        System.out.println("---");
        System.out.println(result);
        System.out.println("---");
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
