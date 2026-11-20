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
package de.ddb.labs.ef2so.playground;

import de.ddb.labs.ef2so.processor.Processor;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Runs the Entity Facts transformation for a sample record.
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
public class MetamorphMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://hub.culturegraph.org/entityfacts/118540238"))
                .header("Accept-Language", "en")
                .timeout(Duration.ofSeconds(10))
                .build();

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Entity Facts returned HTTP " + response.statusCode());
            }
            System.out.println(new Processor().process(response.body()));
        }
    }
}
