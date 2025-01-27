package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class IOHandler {
    public final String RESTAURANT_URL;
    public final String ORDER_URL;
    public final String CENTRAL_AREA_URL;
    public final String NO_FLY_ZONE_URL;
    public final String OUTPUT_FOLDER_NAME;
    public final LngLat APPLETON_TOWER;
    private Restaurant[] restaurants;
    private Order[] orders;
    private NamedRegion centralArea;
    private NamedRegion[] noFlyZones;
    private LocalDate date;
    ObjectMapper mapper = new ObjectMapper();

    public NamedRegion getCentralArea() {
        return centralArea;
    }

    public NamedRegion[] getNoFlyZones() {
        return noFlyZones;
    }

    public void setNoFlyZones(NamedRegion[] noFlyZones) {
        this.noFlyZones = noFlyZones;
    }
    public Restaurant[] getRestaurants() {
        return restaurants;
    }
    public Order[] getOrders() {
        return orders;
    }
    public void setOrders(Order[] orders) {
        this.orders = orders;
    }

    private static final Logger logger = LogManager.getLogger();

    public IOHandler(){
        this.restaurants = null;
        this.orders = null;
        this.centralArea = null;
        this.noFlyZones = null;
        this.date = null;
        RESTAURANT_URL = "restaurants";
        ORDER_URL = "orders";
        CENTRAL_AREA_URL = "centralArea";
        NO_FLY_ZONE_URL = "noFlyZones";
        OUTPUT_FOLDER_NAME = "resultfiles/";
        APPLETON_TOWER = new LngLat(-3.186874, 55.944494);
    }

    // Allows the default constants to be changed when testing
    public IOHandler(String restaurantUrl, String orderUrl, String centralAreaUrl, String noFlyZone, String outputFolderName, LngLat appletonTower){
        this.restaurants = null;
        this.orders = null;
        this.centralArea = null;
        this.noFlyZones = null;
        this.date = null;
        RESTAURANT_URL = restaurantUrl;
        ORDER_URL = orderUrl;
        CENTRAL_AREA_URL = centralAreaUrl;
        NO_FLY_ZONE_URL = noFlyZone;
        OUTPUT_FOLDER_NAME = outputFolderName;
        APPLETON_TOWER = appletonTower;
    }
    /**
     * This method will fill the IOHandler object's attributes with the deserialised data from the REST service.
     * @param baseUrl is the base URL that is used to access each subdirectory in the domain
     * @param date is the date used to retrieve orders
     */
    public void readRestData(String baseUrl, LocalDate date){
        logger.info("Reading from {} on date {}", baseUrl, date);
        mapper.registerModule(new JavaTimeModule());
        this.date = date;

        try {
            restaurants = mapper.readValue(new URL(baseUrl + RESTAURANT_URL), Restaurant[].class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading restaurants from REST service");
        }

        try {
            orders = mapper.readValue(new URL(baseUrl + ORDER_URL + "/" + date), Order[].class);
            if (orders.length < 1){
                throw new RuntimeException("No orders on: " + date);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading orders from REST service");
        }

        try {
            centralArea = mapper.readValue(new URL(baseUrl + CENTRAL_AREA_URL), NamedRegion.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading centralArea from REST service");
        }

        try {
            noFlyZones = mapper.readValue(new URL(baseUrl + NO_FLY_ZONE_URL), NamedRegion[].class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading noFlyZones from REST service");
        }
    }
    /**
     * This method will fill the IOHandler object's attributes with the deserialised data from the REST service.
     * @param orderValidator is the orderValidator object used in the method
     * @param pathFinder is the pathFinder object used in the method
     */
    public void writeOutputFiles(OrderValidator orderValidator, PathFinder pathFinder){
        try {
            Files.createDirectories(Paths.get("./" + OUTPUT_FOLDER_NAME));
        } catch (Exception e){
            logger.error("Error creating output folder");
        }

        // Since there may be multiple orders to the same restaurant within a day, it would be more efficient to reuse
        // previously cached paths rather than recalculate them for every order. This saves time (less calculation) and
        // memory (the same node objects are reused rather than creating new ones).
        // Since Restaurant is a record, its hashcode function does not need to be overwritten.
        HashMap<Restaurant, ArrayList<Node>> cachedPaths = new HashMap<>();

        ArrayList<Delivery> deliveries = new ArrayList<>();
        ArrayList<Move> droneMoves = new ArrayList<>();
        ArrayList<LngLatAlt> geojsonCoords = new ArrayList<>();
        ArrayList<Node> fullPath = new ArrayList<>();
        for (Order order: orders){
            deliveries.add(new Delivery(order.getOrderNo(), order.getOrderStatus(), order.getOrderValidationCode(), order.getPriceTotalInPence()));
            if (order.getOrderStatus() != OrderStatus.DELIVERED){
                continue;
            }
            Restaurant restaurant = orderValidator.restaurantWithPizza(order.getPizzasInOrder()[0], restaurants);
            ArrayList<Node> path;
            // If the path has already been calculated, retrieve it
            if (cachedPaths.containsKey(restaurant)){
                path = cachedPaths.get(restaurant);
            }
            // Else calculate the path
            else {
                // The pathfinder returns the reversed path so that the path does not need to be reversed twice
                ArrayList<Node> pathToRestaurant = pathFinder.findShortestPath(APPLETON_TOWER, restaurant.location(), noFlyZones, centralArea);

                if (pathToRestaurant == null){
                    // This error is non-critical, so the program does not need to stop
                    System.err.println("The path for order: " + order.getOrderNo() + " could not be calculated");
                    continue;
                }

                // Creates a shallow copy of the restaurant to AT
                path = new ArrayList<>(pathToRestaurant);
                // A hover node is added at the end where the restaurant is and the hover node at the start (appleton tower) is removed
                Node hoverNode = new Node(restaurant.location());
                hoverNode.directionFromParent = 999;
                pathToRestaurant.add(hoverNode);
                pathToRestaurant.remove(0);
                // The path from AT to the restaurant is reversed then added to path which gives the path from AT to the
                // restaurant and back
                Collections.reverse(pathToRestaurant);
                path.addAll(pathToRestaurant);
                // Adds a new node that represents the drone hovering when it arrives back to appleton tower

                // Store the path in the cache for future use
                cachedPaths.put(restaurant, path);
            }

            // Adds the current order's path to the full path of orders in the day
            fullPath.addAll(path);
        }

        try{
            mapper.writeValue(new File("./" + OUTPUT_FOLDER_NAME + "deliveries-" + date + ".json"), deliveries);
        } catch (Exception e) {
            logger.error("Error writing deliveries");
        }
        logger.info("Outputted deliveries to {}deliveries-{}.json", OUTPUT_FOLDER_NAME, date);

        // Removes the hover node at the start of the path
        fullPath.remove(0);
        // Adds a hover node at the end of the path
        Node hoverNode = new Node(APPLETON_TOWER);
        hoverNode.directionFromParent = 999;
        fullPath.add(hoverNode);
        // The return path needs to have its angle flipped, so this flag will alternate each time the drone hovers
        // (indicating that it has changed directions).
        // This is done instead of creating new nodes for the return path (make a deep copy instead of shallow) to save memory.
        boolean reversedPath = false;
        for (int i = 0; i < fullPath.size(); i++){
            Move move;
            LngLatAlt coords = new LngLatAlt(fullPath.get(i).pos.lng(), fullPath.get(i).pos.lat());
            if (fullPath.get(i).directionFromParent == 999){
                move = new Move(fullPath.get(i).pos, fullPath.get(i).pos, 999);
                reversedPath = !reversedPath;
            }
            else if (reversedPath){
                move = new Move(fullPath.get(i).pos, fullPath.get(i+1).pos, (fullPath.get(i).directionFromParent + 180) % 360);
            }
            else {
                move = new Move(fullPath.get(i).pos, fullPath.get(i+1).pos, fullPath.get(i).directionFromParent);
            }
            droneMoves.add(move);
            geojsonCoords.add(coords);
        }

        try{
            mapper.writeValue(new File("./" + OUTPUT_FOLDER_NAME + "flightpath-" + date + ".json"), droneMoves);
        } catch (Exception e){
            logger.error("Error writing flightpath");
        }
        logger.info("Outputted flightpath to {}flightpath-{}.json", OUTPUT_FOLDER_NAME, date);

        FeatureCollection featureCollection = new FeatureCollection();
        Feature feature = new Feature();
        feature.setGeometry(new LineString(geojsonCoords.toArray(LngLatAlt[]::new)));
        featureCollection.add(feature);

        try{
            mapper.writeValue(new File("./" + OUTPUT_FOLDER_NAME + "drone-" + date + ".geojson"), featureCollection);
        } catch (Exception e){
            logger.error("Error writing geojson file");
        }
        logger.info("Outputted geojson to {}geojson-{}.geojson", OUTPUT_FOLDER_NAME, date);
    }
}
