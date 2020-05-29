/*
 * Copyright 2018, 2019 Michael Büchner, Deutsche Digitale Bibliothek.
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
package de.ddb.labs.ef2so.jerseyfilter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Provider
public class CORSFilter implements ContainerResponseFilter {

    /**
     * Adds a CORS header the all responses
     *
     * @param request HTTP Request
     * @param response
     * @return
     */
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        String allowOrigin = "*";
        if (request.getHeaderValue("Origin") != null && !request.getHeaderValue("Origin").isEmpty()) {
            allowOrigin = request.getHeaderValue("Origin");
        }

        response.getHttpHeaders().add("Access-Control-Allow-Origin", allowOrigin);
        response.getHttpHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");
        response.getHttpHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHttpHeaders().add("Access-Control-Allow-Methods", "GET");
        response.getHttpHeaders().add("Access-Control-Max-Age", "1728000");

        return response;
    }
}

