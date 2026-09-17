package org.example.wayveesystem.dto.response;

import java.util.List;


public record OverpassResponse(
        List<OverpassElementResponse> elements
) {}
