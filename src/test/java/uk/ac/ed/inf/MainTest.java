package uk.ac.ed.inf;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MainTest {
    String baseUrl = "http://localhost:8082/";
    String date = "2025-01-13";
    String[] args = {date, baseUrl};
    static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8082));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8082);

        configureMockServer.configure(wireMockServer);
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testNormal(){
        Main.main(args);
    }
}
