package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TripValidationRequest(
        @NotBlank(message = "TRIP_NAME_REQUIRED")
        @Size(min = 2, max = 100, message = "Tên chuyến đi phải từ 2 đến 100 ký tự")
        String tripName,

        @NotBlank(message = "TRIP_DATES_REQUIRED")
        String tripDates,

        @NotBlank(message = "DESTINATION_REQUIRED")
        String destination,

        @NotBlank(message = "COMPANIONS_REQUIRED")
        String companions,

        @NotBlank(message = "PASSENGER_COUNT_REQUIRED")
        String passengerCount,

        @NotBlank(message = "TRAVEL_STYLE_REQUIRED")
        String travelStyle
) {}
