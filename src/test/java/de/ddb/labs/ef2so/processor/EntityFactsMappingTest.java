package de.ddb.labs.ef2so.processor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityFactsMappingTest {

    @ParameterizedTest
    @CsvSource({"http, male, Male", "http, female, Female", "https, male, Male", "https, female, Female"})
    void mapsGenderFromLegacyAndCurrentEntityFactsUris(String scheme, String gender, String expected) {
        final String input = """
                {
                  "@id": "https://d-nb.info/gnd/118540238",
                  "@type": "person",
                  "gender": { "@id": "%s://d-nb.info/standards/vocab/gnd/gender#%s" }
                }
                """.formatted(scheme, gender);

        final String result = new Processor().process(input);

        assertTrue(result.contains("\"gender\":\"http://schema.org/" + expected + "\""), result);
    }
}
