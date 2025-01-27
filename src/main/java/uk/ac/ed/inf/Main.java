package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Main {
    private static final Logger logger = LogManager.getLogger();

    public static void main( String[] args ) {
        if (args.length != 2){
            logger.fatal("There must be 2 arguments");
            System.exit(1);
        }

        LocalDate date = null;
        try{
            date = LocalDate.parse(args[0]);
        } catch (DateTimeParseException e) {
            logger.fatal("The first argument provided is not in a valid date format");
            System.exit(2);
        }

        String baseUrl = args[1];
        if (!baseUrl.endsWith("/")){
            baseUrl += "/";
        }

        try{
            new URL(baseUrl).toURI();
        } catch (Exception e) {
            logger.fatal("{} is not a valid URL", baseUrl);
            System.exit(3);
        }

        IOHandler ioHandler = new IOHandler();
        try {
            ioHandler.readRestData(baseUrl, date);
        } catch (RuntimeException e){
            logger.fatal("failed to read rest data: {}", String.valueOf(e));
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
            logger.fatal("Failed to write files: {}", String.valueOf(e));
            System.exit(6);
        }
    }
}
