package com.sang.sourcepattern.dto.response.goong;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoongDirectionsResponse {
    
    @JsonProperty("routes")
    List<Route> routes;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        @JsonProperty("overview_polyline")
        OverviewPolyline overviewPolyline;
        
        @JsonProperty("legs")
        List<Leg> legs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverviewPolyline {
        @JsonProperty("points")
        String points;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        @JsonProperty("distance")
        GoongDistanceMatrixResponse.Distance distance;
        
        @JsonProperty("duration")
        GoongDistanceMatrixResponse.Duration duration;
        
        @JsonProperty("steps")
        List<Step> steps;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        @JsonProperty("html_instructions")
        String htmlInstructions;
        
        @JsonProperty("distance")
        GoongDistanceMatrixResponse.Distance distance;
        
        @JsonProperty("duration")
        GoongDistanceMatrixResponse.Duration duration;
    }
}
