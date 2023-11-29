package uk.ac.ed.inf;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;

import java.time.LocalDate;

public class IOHandlerTests {
    String baseUrl = "https://ilp-rest.azurewebsites.net/";
    LocalDate date = LocalDate.parse("2023-11-27");
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
