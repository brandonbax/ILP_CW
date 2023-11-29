package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Hello world!
 *
 */
public class Main {

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
            System.exit(2);
        }

        String baseUrl = args[1];
        if (!baseUrl.endsWith("/")){
            baseUrl += "/";
        }

        try{
            new URL(baseUrl).toURI();
        } catch (Exception e) {
            System.err.println(baseUrl + " is not a valid URL");
            System.exit(3);
        }

        IOHandler ioHandler = new IOHandler();
        try {
            ioHandler.readRestData(baseUrl, date);
        } catch (RuntimeException e){
            System.err.println("failed to read rest data: " + e);
            System.exit(4);
        }

        Restaurant[] restaurants = ioHandler.getRestaurants();
        Order[] orders = ioHandler.getOrders();

        OrderValidator orderValidator = new OrderValidator();
        try{
            for (Order order: orders){
                orderValidator.validateOrder(order, restaurants);
            }
        } catch (RuntimeException e) {
            System.err.println("Null values are present in the data in the rest service");
            System.exit(5);
        }

        PathFinder pathFinder = new PathFinder();

        try {
            ioHandler.writeOutputFiles(orderValidator, pathFinder);
        } catch (RuntimeException e){
            System.err.println("Failed to write files: " + e);
            System.exit(6);
        }
    }
}
