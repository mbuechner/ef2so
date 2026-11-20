package de.ddb.labs.ef2so.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.metafacture.framework.MetafactureException;

class ProcessorFactoryTest {

    private static final String PERSON = """
            {"@id":"https://d-nb.info/gnd/118540238","@type":"person","preferredName":"Goethe, Johann Wolfgang von"}
            """;

    @Test
    void replacesProcessorsAfterMalformedInputWithoutLosingCapacity() {
        final ProcessorFactory factory = ProcessorFactory.getInstance();
        for (int attempt = 0; attempt < 129; attempt++) {
            assertThrows(MetafactureException.class, () -> factory.tryProcess("""
                    {"@type":"person","variantName":["Incomplete"
                    """));
        }

        final String result = factory.tryProcess(PERSON).orElseThrow();
        assertTrue(result.contains("\"@type\":\"Person\""));
        assertTrue(result.contains("\"name\":\"Goethe, Johann Wolfgang von\""));
        assertEquals(result, factory.tryProcess(PERSON).orElseThrow());
    }

    @Test
    void unsupportedRecordsDoNotReusePreviousContent() {
        final Processor processor = new Processor();
        final String result = processor.process(PERSON);
        processor.reset();
        assertEquals("", processor.process("""
                {"@type":"family","preferredName":"Example family"}
                """));
        processor.reset();
        assertEquals(result, processor.process(PERSON));
    }
}
