package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
            System.out.println("read all restaurants");
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

        OrderValidation orderValidation = new OrderValidator();
        try{
            assert orders != null;
            for (Order order: orders){
                orderValidation.validateOrder(order, restaurants);
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
    }
}
