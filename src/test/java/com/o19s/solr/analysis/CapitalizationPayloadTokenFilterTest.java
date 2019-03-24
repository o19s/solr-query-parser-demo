package com.o19s.solr.analysis;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CapitalizationPayloadTokenFilterTest {

    @Test
    public void testCapitalizationPayloadEnumWithValidOperatorName() {
        Optional<CapitalizationPayloadEnum> capEnumOptional = CapitalizationPayloadEnum.getPayloadForOperator("firstcap");
        assertTrue(capEnumOptional.isPresent());
        assertEquals(CapitalizationPayloadEnum.FIRSTCAP, capEnumOptional.get());

        capEnumOptional = CapitalizationPayloadEnum.getPayloadForOperator("Cap");
        assertTrue(capEnumOptional.isPresent());
        assertEquals(CapitalizationPayloadEnum.CAP, capEnumOptional.get());

        capEnumOptional = CapitalizationPayloadEnum.getPayloadForOperator("ALLCAP");
        assertTrue(capEnumOptional.isPresent());
        assertEquals(CapitalizationPayloadEnum.ALLCAP, capEnumOptional.get());

    }

    @Test
    public void testCapitalizationPayloadEnumWithInvalidOperatorName() {
        Optional<CapitalizationPayloadEnum> capEnumOptional = CapitalizationPayloadEnum.getPayloadForOperator("bogus");
        assertFalse(capEnumOptional.isPresent());
    }
}
