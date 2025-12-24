package com.RRBank.banking.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Flexible LocalDate Deserializer
 * Supports multiple date formats for user convenience
 * 
 * Supported formats:
 * - yyyy-MM-dd (ISO standard: 1999-05-08)
 * - MM-dd-yyyy (US format: 05-08-1999)
 * - dd-MM-yyyy (European format: 08-05-1999)
 * - yyyy/MM/dd (1999/05/08)
 * - MM/dd/yyyy (05/08/1999)
 * - dd/MM/yyyy (08/05/1999)
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),    // ISO: 1999-05-08
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),    // US: 05-08-1999
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),    // EU: 08-05-1999
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),    // 1999/05/08
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),    // 05/08/1999
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),    // 08/05/1999
            DateTimeFormatter.ofPattern("d-M-yyyy"),      // 8-5-1999
            DateTimeFormatter.ofPattern("M-d-yyyy"),      // 5-8-1999
            DateTimeFormatter.ofPattern("yyyy-M-d")       // 1999-5-8
    );

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String dateString = parser.getText().trim();
        
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        // Try each formatter until one works
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        // If no format worked, throw a helpful error
        throw new IOException(
                "Invalid date format: '" + dateString + "'. " +
                "Supported formats: yyyy-MM-dd (e.g., 1999-05-08), " +
                "MM-dd-yyyy (e.g., 05-08-1999), dd-MM-yyyy (e.g., 08-05-1999)"
        );
    }
}
