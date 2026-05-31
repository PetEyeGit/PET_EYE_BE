package com.sang.sourcepattern.dto.response.goong;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LatLong {
    Double latitude;
    Double longitude;
}
