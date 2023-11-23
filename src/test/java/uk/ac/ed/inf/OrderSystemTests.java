package uk.ac.ed.inf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class OrderSystemTests {
    OrderValidation orderValidator;
    Pizza pizza1;
    Pizza pizza2;
    CreditCardInformation creditInfo;
    Restaurant restaurant1;
    @BeforeEach
    void setUp(){
        orderValidator = new OrderValidator();
        pizza1 = new Pizza("pizza1", 50);
        pizza2 = new Pizza("pizza2", 100);
        creditInfo = new CreditCardInformation("1234567891234567", "09/28", "123");
        restaurant1 = new Restaurant("restaurant1", new LngLat(1, 1), new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY}, new Pizza[]{pizza1, pizza2});
    }

    @Test
    public void testValid(){
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.NO_ERROR);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.DELIVERED);
    }

    @Test
    public void testInvalidCardNum(){
        creditInfo.setCreditCardNumber("1234");
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.CARD_NUMBER_INVALID);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }

    @Test
    public void testInvalidExpDate(){
        creditInfo.setCreditCardExpiry("75");
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.EXPIRY_DATE_INVALID);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testOldExpDate(){
        creditInfo.setCreditCardExpiry("12/01");
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.EXPIRY_DATE_INVALID);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testInvalidCvv(){
        creditInfo.setCvv("12");
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.CVV_INVALID);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testTotalIncorrect(){
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 160, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.TOTAL_INCORRECT);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testUndefinedPizza(){
        Pizza pizza3 = new Pizza("pizza3", 100);
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza3}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.PIZZA_NOT_DEFINED);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testTooManyPizza(){
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2, pizza1, pizza2, pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testMultipleRestaurants(){
        Pizza pizza3 = new Pizza("pizza3", 100);
        Restaurant restaurant2 = new Restaurant("restaurant2", new LngLat(1, 2), new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY}, new Pizza[]{pizza3});

        Order order = new Order("1", LocalDate.of(2023, 10, 10), 250, new Pizza[]{pizza1, pizza2, pizza3}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant1, restaurant2});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
    @Test
    public void testRestaurantClosed(){
        Restaurant restaurant2 = new Restaurant("restaurant2", new LngLat(1, 1), new DayOfWeek[]{DayOfWeek.MONDAY}, new Pizza[]{pizza1, pizza2});
        Order order = new Order("1", LocalDate.of(2023, 10, 10), 150, new Pizza[]{pizza1, pizza2}, creditInfo);
        orderValidator.validateOrder(order, new Restaurant[]{restaurant2});

        Assertions.assertEquals(order.getOrderValidationCode(), OrderValidationCode.RESTAURANT_CLOSED);
        Assertions.assertEquals(order.getOrderStatus(), OrderStatus.INVALID);
    }
}
