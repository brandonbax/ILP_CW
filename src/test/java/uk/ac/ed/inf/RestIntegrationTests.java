package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

public class RestIntegrationTests {
    static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8080);

        configureMockServer.configure(wireMockServer);

    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void testGetOrdersByDate() throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/orders/2025-01-13"))
                .GET()
                .build();

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Deserialize the JSON response into a list of Order objects
        Order[] orders = mapper.readValue(response.body(), Order[].class);

        String firstOrderId = orders[0].getOrderNo();
        Assertions.assertEquals("0C65E619", firstOrderId);
    }

    @Test
    public void testGetRestaurants() throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/restaurants"))
                .GET()
                .build();

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Deserialize the JSON response into a list of Order objects
        Restaurant[] restaurants = mapper.readValue(response.body(), Restaurant[].class);

        String firstRestaurantName = restaurants[0].name();
        Assertions.assertEquals("Civerinos Slice", firstRestaurantName);
    }

    @Test
    public void testGetNoFlyZones() throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/noFlyZones"))
                .GET()
                .build();

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Deserialize the JSON response into a list of Order objects
        NamedRegion[] noFlyZones = mapper.readValue(response.body(), NamedRegion[].class);

        String firstNoFlyZoneName = noFlyZones[0].name();
        Assertions.assertEquals("George Square Area", firstNoFlyZoneName);
    }

    @Test
    public void testGetCentralArea() throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/centralArea"))
                .GET()
                .build();

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Deserialize the JSON response into a list of Order objects
        NamedRegion centralArea = mapper.readValue(response.body(), NamedRegion.class);

        String centralAreaName = centralArea.name();
        Assertions.assertEquals("central", centralAreaName);
    }
}
