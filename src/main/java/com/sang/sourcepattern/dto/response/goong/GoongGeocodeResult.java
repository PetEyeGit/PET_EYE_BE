package com.sang.sourcepattern.dto.response.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoongGeocodeResult {
    
    @JsonProperty("formatted_address")
    String formattedAddress;
    
    @JsonProperty("geometry")
    Geometry geometry;
}
