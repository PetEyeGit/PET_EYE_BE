package com.sang.sourcepattern.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.sourcepattern.dto.response.goong.LatLong;
import com.sang.sourcepattern.dto.response.goong.GoongDirectionsResponse;
import com.sang.sourcepattern.dto.response.goong.GoongDistanceMatrixResponse;
import com.sang.sourcepattern.dto.response.goong.GoongGeocodeResponse;
import com.sang.sourcepattern.dto.response.goong.GoongGeocodeResult;
import com.sang.sourcepattern.service.GoongMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoongMapServiceImpl implements GoongMapService {

    private final RestTemplate restTemplate;

    @Value("${goong.api.key}")
    private String goongApiKey;

    @Value("${goong.geocode.url}")
    private String geocodeUrl;

    @Value("${goong.distance.matrix.url}")
    private String distanceMatrixUrl;

    @Override
    public LatLong geocodeAddress(String address) {
        try {
            String url = UriComponentsBuilder.fromUriString(geocodeUrl)
                    .queryParam("address", address)
                    .queryParam("api_key", goongApiKey)
                    .build()
                    .toUriString();

            ResponseEntity<GoongGeocodeResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildRequestEntity(),
                    GoongGeocodeResponse.class
            );

            GoongGeocodeResponse geocodeResponse = response.getBody();

            if (geocodeResponse != null && geocodeResponse.getResults() != null 
                    && !geocodeResponse.getResults().isEmpty()) {
                GoongGeocodeResult result = geocodeResponse.getResults().get(0);
                double lat = result.getGeometry().getLocation().getLat();
                double lng = result.getGeometry().getLocation().getLng();
                return new LatLong(lat, lng);
            }

            return null;

        } catch (Exception e) {
            log.error("[Goong] Geocoding failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public double getDistanceKm(LatLong origin, LatLong destination) {
        try {
            String url = String.format(
                    "%s?origins=%s,%s&destinations=%s,%s&api_key=%s",
                    distanceMatrixUrl,
                    origin.getLatitude(), origin.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(),
                    goongApiKey
            );

            ResponseEntity<GoongDistanceMatrixResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildRequestEntity(),
                    GoongDistanceMatrixResponse.class
            );

            GoongDistanceMatrixResponse body = response.getBody();
            if (body != null && body.getRows() != null && !body.getRows().isEmpty()
                    && body.getRows().get(0).getElements() != null
                    && !body.getRows().get(0).getElements().isEmpty()) {
                
                double meters = body.getRows().get(0).getElements().get(0)
                        .getDistance().getValue();
                return meters / 1000.0;
            }

            return Double.MAX_VALUE;

        } catch (Exception e) {
            log.error("[Goong] Distance calculation failed: {}", e.getMessage());
            return Double.MAX_VALUE;
        }
    }

    @Override
    public GoongDirectionsResponse getDirections(LatLong origin, LatLong destination) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://rsapi.goong.io/Direction")
                    .queryParam("origin", origin.getLatitude() + "," + origin.getLongitude())
                    .queryParam("destination", destination.getLatitude() + "," + destination.getLongitude())
                    .queryParam("vehicle", "car")
                    .queryParam("api_key", goongApiKey)
                    .build()
                    .toUriString();

            ResponseEntity<GoongDirectionsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildRequestEntity(),
                    GoongDirectionsResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("[Goong] Directions failed: {}", e.getMessage());
            return null;
        }
    }

    private HttpEntity<String> buildRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
}
