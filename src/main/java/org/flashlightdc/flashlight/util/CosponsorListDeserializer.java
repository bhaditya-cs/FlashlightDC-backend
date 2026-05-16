package org.flashlightdc.flashlight.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flashlightdc.flashlight.dto.CosponsorDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CosponsorListDeserializer extends JsonDeserializer<List<CosponsorDto>> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<CosponsorDto> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        JsonNode node = mapper.readTree(p);
        JsonNode items = node.get("item");
        if (items == null || items.isNull()) return List.of();
        List<CosponsorDto> result = new ArrayList<>();
        for (JsonNode item : items) {
            result.add(mapper.treeToValue(item, CosponsorDto.class));
        }
        return result;
    }
}