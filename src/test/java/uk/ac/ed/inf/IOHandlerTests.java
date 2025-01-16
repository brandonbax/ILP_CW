package uk.ac.ed.inf;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;

import java.time.LocalDate;

public class IOHandlerTests {
    String baseUrl = "http://localhost:8083/";
    LocalDate date = LocalDate.parse("2025-01-13");
    static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8083));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8083);

        configureMockServer.configure(wireMockServer);
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void readRestData(){
        IOHandler ioHandler = new IOHandler();
        ioHandler.readRestData(baseUrl, date);
        assert ioHandler.getRestaurants() != null;
        assert ioHandler.getOrders() != null;
        assert ioHandler.getNoFlyZones() != null;
        assert ioHandler.getCentralArea() != null;
    }
    @Test
    void invalidRestaurantUrl(){
        IOHandler ioHandler = new IOHandler("123", "orders", "centralArea", "noFlyZones", "resultfiles/", new LngLat(-3.186874, 55.944494));
        try {
            ioHandler.readRestData(baseUrl, date);
            Assertions.fail("An exception should have been thrown");
        } catch (Exception ex){
            Assertions.assertEquals(RuntimeException.class, ex.getClass());
            Assertions.assertEquals("Error reading restaurants from REST service", ex.getMessage());
        }
    }
    @Test
    void invalidOrdersUrl(){
        IOHandler ioHandler = new IOHandler("restaurants", "123", "centralArea", "noFlyZones", "resultfiles/", new LngLat(-3.186874, 55.944494));
        try {
            ioHandler.readRestData(baseUrl, date);
            Assertions.fail("An exception should have been thrown");
        } catch (Exception ex){
            Assertions.assertEquals(RuntimeException.class, ex.getClass());
            Assertions.assertEquals("Error reading orders from REST service", ex.getMessage());
        }
    }
    @Test
    void invalidCentralAreaUrl(){
        IOHandler ioHandler = new IOHandler("restaurants", "orders", "123", "noFlyZones", "resultfiles/", new LngLat(-3.186874, 55.944494));
        try {
            ioHandler.readRestData(baseUrl, date);
            Assertions.fail("An exception should have been thrown");
        } catch (Exception ex){
            Assertions.assertEquals(RuntimeException.class, ex.getClass());
            Assertions.assertEquals("Error reading centralArea from REST service", ex.getMessage());
        }
    }
    @Test
    void invalidNoFlyZonesUrl(){
        IOHandler ioHandler = new IOHandler("restaurants", "orders", "centralArea", "123", "resultfiles/", new LngLat(-3.186874, 55.944494));
        try {
            ioHandler.readRestData(baseUrl, date);
            Assertions.fail("An exception should have been thrown");
        } catch (Exception ex){
            Assertions.assertEquals(RuntimeException.class, ex.getClass());
            Assertions.assertEquals("Error reading noFlyZones from REST service", ex.getMessage());
        }
    }
}
