package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PathFindingTests {
    ObjectMapper mapper = new ObjectMapper();
    static IOHandler ioHandler = new IOHandler();
    static LocalDate date = LocalDate.parse("2025-01-13");
    static LngLatHandler lngLatHandler = new LngLatHandler();
    static OrderValidator orderValidator = new OrderValidator();
    static PathFinder pathFinder = new PathFinder();
    static WireMockServer wireMockServer;

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8081));
        wireMockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", 8081);

        ConfigureMockServer.configure(wireMockServer);
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @BeforeEach
    void generateResults() {
        ioHandler.readRestData("http://localhost:8081/", date);
    }

    @Test
    void noPointsInNoFlyZones(){
        ioHandler.writeOutputFiles(orderValidator, pathFinder);
        Move[] flightpath = null;
        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            Assertions.fail();
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
        LngLat[] points1 = {new LngLat(-3.193112571586539, 55.9457), new LngLat(-3.193112571586539, 55.942749545362716),
                new LngLat(-3.191488352673872, 55.942749545362716), new LngLat(-3.191488352673872, 55.9457), new LngLat(-3.193112571586539, 55.9457)};
        NamedRegion[] newNoFlyZones = {new NamedRegion("new no-fly", points1)};
        ioHandler.setNoFlyZones(newNoFlyZones);
        ioHandler.writeOutputFiles(orderValidator, pathFinder);

        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            Assertions.fail();
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

        ioHandler.writeOutputFiles(orderValidator, pathFinder);

        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            Assertions.fail();
        }

        // Checks that there are no points in the no-fly zone
        for (Move move: flightpath){
            for (NamedRegion noFlyZone: ioHandler.getNoFlyZones()) {
                assert (!lngLatHandler.isInRegion(move.startPos(), noFlyZone));
                assert (!lngLatHandler.isInRegion(move.endPos(), noFlyZone));
            }
        }
    }

    @Test
    @Timeout(value=60, unit=TimeUnit.SECONDS)
    void multiOrderHard(){
        Move[] flightpath = null;
        // Adds additional no-fly zones to make the path finding harder
        LngLat[] points1 = {new LngLat(-3.193112571586539, 55.9457), new LngLat(-3.193112571586539, 55.942749545362716),
                new LngLat(-3.191488352673872, 55.942749545362716), new LngLat(-3.191488352673872, 55.9457), new LngLat(-3.193112571586539, 55.9457)};
        LngLat[] points2 = {new LngLat(-3.186251897548658, 55.94394452272155), new LngLat(-3.186261183964149, 55.94262879226724),
                new LngLat(-3.1835588370524874, 55.942597588631), new LngLat(-3.1836145555453754, 55.943923721006854), new LngLat(-3.186251897548658, 55.94394452272155)};
        LngLat[] points3 = {new LngLat(-3.1905881363705078, 55.94421632305668), new LngLat(-3.190541704292201, 55.94238054159774),
                new LngLat(-3.187050012062116, 55.943878296179804), new LngLat(-3.1887401396847395, 55.9453655923692), new LngLat(-3.1905881363705078, 55.94421632305668)};
        NamedRegion[] newNoFlyZones = {new NamedRegion("new no-fly", points1), new NamedRegion("new no-fly 2", points2), new NamedRegion("new no-fly 3", points3)};
        ioHandler.setNoFlyZones(newNoFlyZones);
        ioHandler.writeOutputFiles(orderValidator, pathFinder);

        try {
            flightpath = mapper.readValue(new File("./resultfiles/flightpath-" + date + ".json"), Move[].class);
        } catch (IOException e) {
            Assertions.fail();
        }

        // Checks that the path is not in a no-fly zone
        for (Move move: flightpath){
            for (NamedRegion noFlyZone: ioHandler.getNoFlyZones()) {
                assert (!lngLatHandler.isInRegion(move.startPos(), noFlyZone));
                assert (!lngLatHandler.isInRegion(move.endPos(), noFlyZone));
            }
        }
    }

    @Test
    void singleIncalculablePath(){
        PathFinder pathFinder = new PathFinder();
        // Tries to find path from AT to the middle of a no-fly zone
        ArrayList<Node> path = pathFinder.findShortestPath(ioHandler.APPLETON_TOWER, new LngLat(-3.1888, 55.9437), ioHandler.getNoFlyZones(), ioHandler.getCentralArea());
        Assertions.assertNull(path);
    }
}
