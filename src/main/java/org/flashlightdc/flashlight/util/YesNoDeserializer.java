package org.flashlightdc.flashlight.util;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class YesNoDeserializer extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        System.out.println("Verify this ran");
        if (value.equalsIgnoreCase("Y")) {
            return true;
        } else if (value.equalsIgnoreCase("N")) {
            return false;
        }
        return false; // Default
    }
}