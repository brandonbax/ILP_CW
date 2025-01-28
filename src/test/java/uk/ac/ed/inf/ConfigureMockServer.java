package uk.ac.ed.inf;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class ConfigureMockServer {
    String noFlyZoneFileName;
    public ConfigureMockServer(String noFlyZoneFileName) {
        this.noFlyZoneFileName = noFlyZoneFileName;
    }

    public void configure(WireMockServer wireMockServer) {
        // Stub for 2025-01-13
        wireMockServer.stubFor(get(urlPathEqualTo("/orders/2025-01-13"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("orders_2025-01-13.json")));

        wireMockServer.stubFor(get(urlPathEqualTo("/orders/2025-01-20"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("many_orders.json")));

        wireMockServer.stubFor(get(urlPathEqualTo("/restaurants"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("restaurants.json")));

        wireMockServer.stubFor(get(urlPathEqualTo("/noFlyZones"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(noFlyZoneFileName)));

        wireMockServer.stubFor(get(urlPathEqualTo("/centralArea"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("centralArea.json")));
    }
}
