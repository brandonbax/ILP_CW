package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

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
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        // Same as the previous regex, but only 3 digits now
        pattern = Pattern.compile("^\\d{3}$");
        matcher = pattern.matcher(orderToValidate.getCreditCardInformation().getCvv());
        if (!matcher.find()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        // Checks that the 1st 2 digits are either a 0 followed by a digit from 1 to 9 or a 1 followed by a digit
        // from 0 to 2. It then checks for a slash followed by 2 digits.
        pattern = Pattern.compile("^(0[1-9]|1[0-2])/\\d{2}$");
        matcher = pattern.matcher(orderToValidate.getCreditCardInformation().getCreditCardExpiry());
        if (!matcher.find()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
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
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        // Checks that the number of pizzas in the order are less than the max allowed
        if (orderToValidate.getPizzasInOrder().length > SystemConstants.MAX_PIZZAS_PER_ORDER){
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        // Gets the restaurant with the first pizza so that the other pizzas can be checked to be in the same restaurant
        Restaurant initialRestaurant = restaurantWithPizza(orderToValidate.getPizzasInOrder()[0], definedRestaurants);
        // If that restaurant is null, then the first pizza is not in a restaurant's menu. This check is done outside
        // the for loop below because there is no need to check for multiple restaurant when there is only 1 pizza
        if (initialRestaurant == null){
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }
        // used the stream method noneMatch to check if the order date is not on any of the restaurants opening days
        if (Arrays.stream(initialRestaurant.openingDays()).noneMatch(day -> orderToValidate.getOrderDate().getDayOfWeek() == day)){
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        for (int i = 1; i < orderToValidate.getPizzasInOrder().length; i++){
            Restaurant currentRestaurant = restaurantWithPizza(orderToValidate.getPizzasInOrder()[i], definedRestaurants);
            if (currentRestaurant == null){
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                return orderToValidate;
            }
            if (currentRestaurant != initialRestaurant){
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                return orderToValidate;
            }
        }

        int totalOrderPrice = 0;
        for (Pizza pizza: orderToValidate.getPizzasInOrder()){
            totalOrderPrice += pizza.priceInPence();
        }

        if (totalOrderPrice != orderToValidate.getPriceTotalInPence()){
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            return orderToValidate;
        }

        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        return orderToValidate;
    }

    /**
     * @param pizza is the pizza that need to have its associated restaurant found
     * @param definedRestaurants is the list of all defined restaurants that will be searched
     * @return the restaurant with the specified pizza or null if there is no such restaurant
     */
    private Restaurant restaurantWithPizza(Pizza pizza, Restaurant[] definedRestaurants){
        for (Restaurant restaurant: definedRestaurants){
            for (Pizza pizzaInMenu: restaurant.menu()){
                if (pizzaInMenu.name().equals(pizza.name())){
                    return restaurant;
                }
            }
        }
        return null;
    }
}
