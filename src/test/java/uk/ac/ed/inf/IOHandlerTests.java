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
        } catch (RuntimeException e){

        }
    }
}
