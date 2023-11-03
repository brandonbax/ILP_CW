package uk.ac.ed.inf;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hello world!
 *
 */
public class Main {
    public static final String RESTAURANT_URL = "restaurants";
    public static final String ORDER_URL = "orders";
    public static final String CENTRAL_AREA_URL = "centralArea";
    public static final String NO_FLY_ZONE_URL = "noFlyZones";
    public static final String IS_ALIVE_URL = "isAlive";
    public static final String OUTPUT_FOLDER_NAME = "resultfiles/";
    public static final LngLat APPLETON_TOWER = new LngLat(-3.186874, 55.944494);

    public static void main( String[] args ) {
        if (args.length != 2){
            System.err.println("There must be 2 arguments");
            System.exit(1);
        }

        LocalDate date = null;
        try{
            date = LocalDate.parse(args[0]);
        } catch (DateTimeParseException e) {
            System.err.println("The first argument provided is not in a valid date format");
            System.exit(1);
        }

        String baseUrl = args[1];
        if (!baseUrl.endsWith("/")){
            baseUrl += "/";
        }

        try{
            new URL(baseUrl).toURI();
        } catch (Exception e) {
            System.err.println(baseUrl + " is not a valid URL");
            System.exit(1);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try {
            boolean isAlive = mapper.readValue(new URL(baseUrl + IS_ALIVE_URL), boolean.class);
            if (!isAlive){
                System.err.println("Rest service is not active");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Error reading the status of the REST service: " + e);
            System.exit(1);
        }

        Restaurant[] restaurants = null;
        try {
            restaurants = mapper.readValue(new URL(baseUrl + RESTAURANT_URL), Restaurant[].class);
        } catch (IOException e) {
            System.err.println("Error reading restaurants from REST service: " + e);
            System.exit(1);
        }

        Order[] orders = null;
        try {
            orders = mapper.readValue(new URL(baseUrl + ORDER_URL + "/" + date), Order[].class);
            if (orders.length < 1){
                System.err.println("No orders on: " + date);
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Error reading orders from REST service: " + e);
            System.exit(1);
        }

        NamedRegion centralArea = null;
        try {
            centralArea = mapper.readValue(new URL(baseUrl + CENTRAL_AREA_URL), NamedRegion.class);
        } catch (IOException e) {
            System.err.println("Error reading centralArea from REST service: " + e);
            System.exit(1);
        }

        NamedRegion[] noFlyZones = null;
        try {
            noFlyZones = mapper.readValue(new URL(baseUrl + NO_FLY_ZONE_URL), NamedRegion[].class);
        } catch (IOException e) {
            System.err.println("Error reading noFlyZones from REST service: " + e);
            System.exit(1);
        }

        OrderValidator orderValidator = new OrderValidator();
        try{
            for (Order order: orders){
                orderValidator.validateOrder(order, restaurants);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Null values are present in the data in the rest service");
            System.exit(1);
        }

        try{
            mapper.writeValue(new File("resultfiles/orders.json"), orders);
        } catch (Exception e) {
            System.err.println("Failed to serialize orders class to json: " + e);
            System.exit(1);
        }

        try {
            Files.createDirectories(Paths.get("/" + OUTPUT_FOLDER_NAME));
        } catch (Exception e){
            System.err.println("Failed to create output folder: " + e);
            System.exit(1);
        }

        ArrayList<Move> droneMoves = new ArrayList<>();
        ArrayList<LngLatAlt> geojsonCoords = new ArrayList<>();
        ArrayList<Node> fullPath = new ArrayList<>();
        for (Order order: orders){
            ArrayList<Node> path = FindPath.findShortestPath(APPLETON_TOWER, orderValidator.restaurantWithPizza(order.getPizzasInOrder()[0], restaurants).location(), noFlyZones);
            if (path == null){
                // This is a non-critical error that could happen if the program is given
                // invalid data, such as a restaurant being inside a no-fly zone.
                System.err.println("Failed to calculate path for order: " + order);
            }
            fullPath.addAll(path);
        }

        for (int i = 0; i < fullPath.size(); i++){
            Move move;
            LngLatAlt coords = new LngLatAlt(fullPath.get(i).pos.lng(), fullPath.get(i).pos.lat());
            if (fullPath.get(i).directionFromParent == 999){
                move = new Move(fullPath.get(i).pos, fullPath.get(i).pos, 999);

            } else {
                move = new Move(fullPath.get(i).pos, fullPath.get(i+1).pos, fullPath.get(i).directionFromParent);
            }
            droneMoves.add(move);
            geojsonCoords.add(coords);
        }

        try{
            mapper.writeValue(new File("/" + OUTPUT_FOLDER_NAME + "flightpath-" + date + ".json"), droneMoves);
        } catch (Exception e){
            System.err.println("Failed to create json file of the path: " + e);
            System.exit(1);
        }

        FeatureCollection featureCollection = new FeatureCollection();
        Feature feature = new Feature();
        feature.setGeometry(new LineString(geojsonCoords.toArray(LngLatAlt[]::new)));
        featureCollection.add(feature);

        try{
            mapper.writeValue(new File("/" + OUTPUT_FOLDER_NAME + "drone-" + date + ".geojson"), featureCollection);
        } catch (Exception e){
            System.err.println("Failed to create geojson file of the path: " + e);
            System.exit(1);
        }
    }
}
