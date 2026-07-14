package com.travelplatform.packages.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * Stores a List<String> as a single JSON-encoded column instead of using
 * @ElementCollection (which creates an anonymous join table and rewrites
 * the entire collection on every update). One reusable converter backs
 * inclusions, exclusions, placesCovered, activities, and imageUrls on
 * TravelPackage — all flat, unordered string lists with no sub-structure.
 *
 * If a query need ever arises (e.g. "find packages including scuba diving"),
 * swap the affected field to a real @OneToMany child entity — the DTO/API
 * contract stays identical either way.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize list to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize JSON to list", e);
        }
    }
}
