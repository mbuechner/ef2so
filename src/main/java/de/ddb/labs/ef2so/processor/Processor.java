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
package de.ddb.labs.ef2so.processor;


import de.ddb.labs.ef2so.metafacture.JsonDecoder;
import org.metafacture.json.JsonEncoder;
import org.metafacture.metamorph.Filter;
import org.metafacture.metamorph.Metamorph;
import org.metafacture.strings.StringConcatenator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Processor {

    private static final String MORPH_FILTER_SCRIPT = "ef2so_filter.xml";
    private static final String MORPH_TRANS_SCRIPT = "ef2so_transformation.xml";
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);
    private final Metamorph trans;
    private final Filter filter;
    private final JsonEncoder jsonEncoder;
    private final JsonDecoder myjsonDecoder;
    private final StringConcatenator stringConcatenator;
    private boolean free;

    public Processor() {
        this.trans = new Metamorph(MORPH_TRANS_SCRIPT);
        this.filter = new Filter(MORPH_FILTER_SCRIPT);
        this.jsonEncoder = new JsonEncoder();
        this.jsonEncoder.setPrettyPrinting(true);

        this.myjsonDecoder = new JsonDecoder();
        this.stringConcatenator = new StringConcatenator();
        this.free = true;

        myjsonDecoder
                .setReceiver(filter)
                .setReceiver(trans)
                .setReceiver(jsonEncoder)
                .setReceiver(stringConcatenator);
    }

    public String process(String input) {
        LOG.debug("Processor {} starts processing...", this.hashCode());
        myjsonDecoder.process(input);
        final String result = stringConcatenator.getString();
        LOG.debug("Processor {} is done.", this.hashCode());
        return result;
    }

    public boolean isFree() {
        return free;
    }

    public void setOccupied() {
        this.free = false;
    }
    
    public void setFree() {
        myjsonDecoder.resetStream();
        this.free = true;
    }

}
