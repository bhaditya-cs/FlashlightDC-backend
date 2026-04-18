package org.flashlightdc.flashlight.util;

import org.flashlightdc.flashlight.dto.TermDto;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

public class TermListDeserializer extends ValueDeserializer<List<TermDto>> {

    @Override
    public List<TermDto> deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        System.out.println("Verify this ran");
        JsonNode node = ctx.readTree(p);
        JsonNode items = node.get("item");
        if (items == null || items.isNull()) return List.of();
        List<TermDto> result = new ArrayList<>();
        for (JsonNode item : items) {
            result.add(ctx.readTreeAsValue(item, TermDto.class));
        }
        return result;
    }
}