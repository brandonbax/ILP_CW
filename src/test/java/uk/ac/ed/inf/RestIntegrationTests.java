package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.Order;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class RestIntegrationTests {
    static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8080);

        // Stub for 2025-01-13
        wireMockServer.stubFor(get(urlPathEqualTo("/orders/2025-01-13"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("orders_2025-01-13.json")));

        // Stub for 2025-01-14
//        wireMockServer.stubFor(get(urlPathEqualTo("/orders/2025-01-14"))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", "application/json")
//                        .withBodyFile("orders_2025-01-14.json")));
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void testGetOrdersByDate() throws Exception {
        // Example: Make a request to the mock API
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

        // Print the response
        System.out.println(response.body());

        // Assertions
        assert response.body().contains("0C65E619");
        assert !response.body().contains("1D45F520");
    }
}
