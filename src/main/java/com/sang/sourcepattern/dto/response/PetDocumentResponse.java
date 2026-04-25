package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetDocumentResponse {
    int id;
    String type;
    String title;
    String content;
    String imageUrl;
    LocalDate recordDate;
}
