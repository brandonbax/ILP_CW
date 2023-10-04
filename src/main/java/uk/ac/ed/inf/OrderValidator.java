package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderValidator implements OrderValidation {
    /**
     * validate an order and deliver a validated version where the
     * OrderStatus and OrderValidationCode are set accordingly.
     *
     * The order validation code is defined in the enum @link uk.ac.ed.inf.ilp.constant.OrderValidationStatus
     *
     * <p>
     * Fields to validate include (among others - for details please see the OrderValidationStatus):
     * <p>
     * number (16 digit numeric)
     * CVV
     * expiration date
     * the menu items selected in the order
     * the involved restaurants
     * if the maximum count is exceeded
     * if the order is valid on the given date for the involved restaurants (opening days)
     *
     * @param orderToValidate    is the order which needs validation
     * @param definedRestaurants is the vector of defined restaurants with their according menu structure
     * @return the validated order
     */
    @Override
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {
        // Checks that the order contains credit card information, since it is possible to create
        // an Order object without it.
        if (orderToValidate.getCreditCardInformation() == null){
            throw new RuntimeException("Order missing credit card info");
        }

        // Regex checks for a string containing only 16 digits from start to finish
        Pattern pattern = Pattern.compile("^\\d{16}$");
        Matcher matcher = pattern.matcher(orderToValidate.getCreditCardInformation().getCreditCardNumber());
        if (!matcher.find()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            return orderToValidate;
        }

        // Same as the previous regex, but only 3 digits now
        pattern = Pattern.compile("^\\d{3}$");
        matcher = pattern.matcher(orderToValidate.getCreditCardInformation().getCvv());
        if (!matcher.find()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            return orderToValidate;
        }

        // Checks that the 1st 2 digits are either a 0 followed by a digit from 1 to 9 or a 1 followed by a digit
        // from 0 to 2. It then checks for a slash followed by 2 digits.
        pattern = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");
        matcher = pattern.matcher(orderToValidate.getCreditCardInformation().getCreditCardExpiry());
        if (!matcher.find()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        String[] monthAndYear = orderToValidate.getCreditCardInformation().getCreditCardExpiry().split("/");
        int month = Integer.parseInt(monthAndYear[0]);
        int year = Integer.parseInt(monthAndYear[1]);
        LocalDate currentDate = LocalDate.now();
        // Since credit card expiration dates do not have a day (effectively the last day of the month),
        // the day is set to the current day so that it does not the comparison (if expDate and currentDate
        // are in the same month and year, current date will not be after expDate).
        LocalDate expDate = LocalDate.of(year, month, currentDate.getDayOfMonth());
        if (currentDate.isAfter(expDate)){
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        // Checks that the number of pizzas in the order are less than the max allowed
        if (orderToValidate.getPizzasInOrder().length > SystemConstants.MAX_PIZZAS_PER_ORDER){
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            return orderToValidate;
        }

        for (Pizza pizza: orderToValidate.getPizzasInOrder()){
            if (numPizzaInMenu(pizza, definedRestaurants) == 0){
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                return orderToValidate;
            }
        }

        return null;
    }

    private int numPizzaInMenu(Pizza pizza, Restaurant[] definedRestaurants){
        int n = 0;
        boolean foundPizzaInRestaurant = false;

        for (Restaurant restaurant: definedRestaurants){
            foundPizzaInRestaurant = false;
            for (Pizza pizzaInMenu: restaurant.menu()){
                if (pizzaInMenu.name().equals(pizza.name())){
                    // To prevent this method from counting duplicate pizza in a single
                    // restaurant as
                    if (foundPizzaInRestaurant){
                        throw new RuntimeException("Duplicate pizza in restaurant menu");
                    }
                    n++;
                }
            }
        }
        return n;
    }
}
