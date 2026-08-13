package com.sang.sourcepattern.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTransactionDateRequest {

    @NotNull
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    LocalDateTime createdAt;

    public static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();

            // Chuẩn hóa khoảng trắng "2026-08-14 12:41:00" -> "2026-08-14T12:41:00"
            if (text.contains(" ") && !text.contains("T")) {
                text = text.replace(" ", "T");
            }

            // Chuẩn hóa nếu thiếu giây "2026-08-14T12:41" -> "2026-08-14T12:41:00"
            if (text.length() == 16 && text.indexOf('T') == 10) {
                text = text + ":00";
            }

            // 1. Thử parse OffsetDateTime nếu chuỗi chứa múi giờ 'Z' hoặc '+'
            if (text.endsWith("Z") || text.contains("+")) {
                try {
                    return OffsetDateTime.parse(text).toLocalDateTime();
                } catch (Exception ignored) {}
            }

            // 2. Thử parse theo chuẩn ISO LocalDateTime (bao gồm cả miliseconds nếu có)
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignored) {}

            // 3. Fallback cuối cùng
            return LocalDateTime.parse(text);
        }
    }
}
