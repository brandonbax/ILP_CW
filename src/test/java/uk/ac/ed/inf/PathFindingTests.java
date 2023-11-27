package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class PathFindingTests {
    ObjectMapper mapper = new ObjectMapper();
    static IOHandler ioHandler = new IOHandler();
    static LocalDate date = LocalDate.parse("2023-11-23");
    static LngLatHandler lngLatHandler = new LngLatHandler();
    static OrderValidator orderValidator = new OrderValidator();
    static PathFinder pathFinder = new PathFinder();
    @BeforeAll
    static void generateResults(){
        ioHandler.readRestData("https://ilp-rest.azurewebsites.net/", date);

        Restaurant[] restaurants = ioHandler.getRestaurants();
        Order[] orders = ioHandler.getOrders();

        try{
            for (Order order: orders){
                orderValidator.validateOrder(order, restaurants);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Null values are present in the data in the rest service");
            System.exit(1);
        }

        ioHandler.writeOutputFiles(orderValidator, pathFinder, lngLatHandler, date);
    }

    @Test
    void noPointsInNoFlyZones(){
        Move[] flightpath = null;
        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            System.err.println("Error reading restaurants from REST service: " + e);
            System.exit(1);
        }

        for (Move move: flightpath){
            for (NamedRegion noFlyZone: ioHandler.getNoFlyZones()) {
                assert (!lngLatHandler.isInRegion(move.startPos(), noFlyZone));
            }
        }
    }

    @Test
    void multiOrderNoFlyAvoidance(){
        Move[] flightpath = null;
        LngLat[] points1 = {new LngLat(-3.193112571586539, 55.9440545969135), new LngLat(-3.193112571586539, 55.942749545362716),
                new LngLat(-3.191488352673872, 55.942749545362716), new LngLat(-3.191488352673872, 55.9440545969135), new LngLat(-3.193112571586539, 55.9440545969135)};
        NamedRegion[] newNoFlyZones = {new NamedRegion("new no-fly", points1)};
        ioHandler.setNoFlyZones(newNoFlyZones);
        ioHandler.writeOutputFiles(orderValidator, pathFinder, lngLatHandler, date);

        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            System.err.println("Error reading restaurants from REST service: " + e);
            System.exit(1);
        }

        for (Move move: flightpath){
            for (NamedRegion noFlyZone: ioHandler.getNoFlyZones()) {
                assert (!lngLatHandler.isInRegion(move.startPos(), noFlyZone));
                assert (!lngLatHandler.isInRegion(move.endPos(), noFlyZone));
            }
        }
    }

    @Test
    void singleOrderNoFlyAvoidance(){
        Move[] flightpath = null;
        // A new no-fly zone is placed directly in the path of this order in order to test the no-fly zone avoidance
        LngLat[] points1 = {new LngLat(-3.193112571586539, 55.9457), new LngLat(-3.193112571586539, 55.942749545362716),
                new LngLat(-3.191488352673872, 55.942749545362716), new LngLat(-3.191488352673872, 55.9457), new LngLat(-3.193112571586539, 55.9457)};
        NamedRegion[] newNoFlyZones = {new NamedRegion("new no-fly", points1)};
        ioHandler.setNoFlyZones(newNoFlyZones);

        Order[] orders = ioHandler.getOrders();
        for (Order order: orders){
            if (order.getOrderStatus() == OrderStatus.DELIVERED){
                Order[] newOrders = {order};
                ioHandler.setOrders(newOrders);
                break;
            }
        }

        ioHandler.writeOutputFiles(orderValidator, pathFinder, lngLatHandler, date);

        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            System.err.println("Error reading restaurants from REST service: " + e);
            System.exit(1);
        }

        // Checks that there are no points in the no-fly zone
        for (Move move: flightpath){
            for (NamedRegion noFlyZone: ioHandler.getNoFlyZones()) {
                assert (!lngLatHandler.isInRegion(move.startPos(), noFlyZone));
                assert (!lngLatHandler.isInRegion(move.endPos(), noFlyZone));
            }
        }
    }
}
