/*
 * Copyright 2017 hbz
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.labs.ef2so.metafacture;

import java.util.Locale;
import org.metafacture.framework.MetafactureException;
import org.metafacture.framework.StreamReceiver;
import org.metafacture.framework.helpers.DefaultObjectPipe;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

/**
 * Decodes a record in JSON format.
 *
 * @author Jens Wille
 *
 */
public final class JsonDecoder extends DefaultObjectPipe<String, StreamReceiver> {

    public static final String DEFAULT_ARRAY_MARKER = "#";
    public static final String DEFAULT_ARRAY_NAME = "#item";
    public static final String DEFAULT_RECORD_ID = "%d";
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private String arrayMarker;
    private String arrayName;
    private String recordId;
    private int recordCount;

    public JsonDecoder() {
        super();

        setArrayMarker(DEFAULT_ARRAY_MARKER);
        setArrayName(DEFAULT_ARRAY_NAME);
        setRecordId(DEFAULT_RECORD_ID);

        resetRecordCount();
    }

    public void setArrayMarker(final String arrayMarker) {
        this.arrayMarker = arrayMarker;
    }

    public String getArrayMarker() {
        return arrayMarker;
    }

    public void setArrayName(final String arrayName) {
        this.arrayName = arrayName;
    }

    public String getArrayName() {
        return arrayName;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordCount(final int recordCount) {
        this.recordCount = recordCount;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void resetRecordCount() {
        setRecordCount(0);
    }

    @Override
    public void process(final String string) {
        assert !isClosed();

        try (JsonParser parser = JSON_FACTORY.createParser(ObjectReadContext.empty(), string)) {
            decode(parser);
        } catch (final JacksonException e) {
            throw new MetafactureException(e);
        }
    }

    @Override
    protected void onResetStream() {
        resetRecordCount();
    }

    private void decode(final JsonParser parser) {
        JsonToken token;
        while ((token = parser.nextToken()) == JsonToken.START_OBJECT) {
            getReceiver().startRecord(String.format(Locale.ROOT, recordId, ++recordCount));
            decodeObject(parser);
            getReceiver().endRecord();
        }

        if (token != null) {
            throw new MetafactureException("Unexpected token '" + token + "' at " + parser.currentLocation());
        }
    }

    private void decodeObject(final JsonParser parser) {
        while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
            decodeValue(parser, parser.currentName(), parser.nextToken());
        }
    }

    private void decodeArray(final JsonParser parser) {
        int arrayCount = 0;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            decodeValue(parser, String.format(Locale.ROOT, arrayName, ++arrayCount), parser.currentToken());
        }
    }

    private void decodeValue(final JsonParser parser, final String name, final JsonToken token) {
        switch (token) {
            case START_OBJECT -> {
                getReceiver().startEntity(name);
                decodeObject(parser);
                getReceiver().endEntity();
            }
            case START_ARRAY -> {
                getReceiver().startEntity(name + arrayMarker);
                decodeArray(parser);
                getReceiver().endEntity();
            }
            case VALUE_NULL -> getReceiver().literal(name, null);
            default -> getReceiver().literal(name, parser.getString());
        }
    }
}
