package uk.ac.ed.inf;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SystemTests {
    static WireMockServer wireMockServer1;
    static WireMockServer wireMockServer2;
    static WireMockServer wireMockServer3;

    @BeforeAll
    public static void setUp() {
        wireMockServer1 = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8088));
        wireMockServer1.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8088);

        ConfigureMockServer configureMockServer1 = new ConfigureMockServer("noFlyEasy.json");
        configureMockServer1.configure(wireMockServer1);

        wireMockServer2 = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
        wireMockServer2.start();

        WireMock.configureFor("localhost", 8089);
        ConfigureMockServer configureMockServer2 = new ConfigureMockServer("noFlyMedium.json");
        configureMockServer2.configure(wireMockServer2);

        wireMockServer3 = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8090));
        wireMockServer3.start();

        WireMock.configureFor("localhost", 8090);
        ConfigureMockServer configureMockServer3 = new ConfigureMockServer("noFlyHard.json");
        configureMockServer3.configure(wireMockServer3);

    }

    @AfterAll
    public static void tearDown() {
        wireMockServer1.stop();
        wireMockServer2.stop();
        wireMockServer3.stop();
    }

    @Test
    void testEasyLowOrders(){
        Main.main(new String[]{"2025-01-13", "http://localhost:8088/"});
    }

    @Test
    void testEasyManyOrders(){
        Main.main(new String[]{"2025-01-20", "http://localhost:8088/"});
    }

    @Test
    void testMediumLowOrders(){
        Main.main(new String[]{"2025-01-13", "http://localhost:8089/"});
    }

    @Test
    void testMediumManyOrders(){
        Main.main(new String[]{"2025-01-20", "http://localhost:8089/"});
    }

    @Test
    void testHardLowOrders(){
        Main.main(new String[]{"2025-01-13", "http://localhost:8090/"});
    }

    @Test
    void testHardManyOrders(){
        Main.main(new String[]{"2025-01-20", "http://localhost:8090/"});
    }
}
