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
package de.ddb.labs.ef2so.filter;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Michael Büchner <m.buechner@dnb.de>
 */
@Provider
@Compress
public class CompressWriterInterceptor implements WriterInterceptor {

    @Context
    private HttpHeaders headers;
    private static final Logger LOG = LoggerFactory.getLogger(CompressWriterInterceptor.class);

    public CompressWriterInterceptor() {
        BrotliLoader.isBrotliAvailable();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {

        try {
            if (headers.getHeaderString("Content-Encoding") != null) {
                final List<String> list = Arrays.asList(headers.getHeaderString("Content-Encoding").split("\\s*,\\s*"));
                final OutputStream outputStream = context.getOutputStream();

                if (list.contains("br")) {
                    context.setOutputStream(new BrotliOutputStream(outputStream));
                    context.getHeaders().add("Content-Encoding", "br");

                } else if (list.contains("gzip")) {
                    context.setOutputStream(new GZIPOutputStream(outputStream));
                    context.getHeaders().add("Content-Encoding", "gzip");

                } else if (list.contains("deflate")) {
                    context.setOutputStream(new DeflaterOutputStream(outputStream));
                    context.getHeaders().add("Content-Encoding", "deflate");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            context.proceed();
        }
    }
}
