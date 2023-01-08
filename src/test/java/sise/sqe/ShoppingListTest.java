package sise.sqe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import java.lang.reflect.*;

class ShoppingListTest {
  private ShoppingList shoppingList;
  private Supermarket superMarket;
  private Product product1;
  private Product product2;

  @BeforeEach
  public void setUp() {
    this.superMarket = Mockito.mock(Supermarket.class);
    this.shoppingList = Mockito.spy(new ShoppingList(superMarket));

    this.product1 = Mockito.spy(new Product("apple", "apple", 10));
    this.product2 = Mockito.spy(new Product("pear", "pear", 10));

    shoppingList.addProduct(product1);
    shoppingList.addProduct(product2);

  }

  @Test
  public void addProduct_success() throws Exception {
    Product newProduct = new Product("newProductId", "newProductName", 10);
    shoppingList.addProduct(newProduct);
    List<Product> products = getProducts();
    assertTrue(products.contains(newProduct), "New product should have been added to the list");
  }

  /*
  Use Cases :
      10 apples for the price of 25 each -> 250
      10 pears for the price of 25 -> 250
      total price -> 500

      10 apples for the price of 10 each -> 100
      10 pears for the price of 5 -> 50
      total price -> 150

      10 apples for the price of 0 each -> 0
      10 pears for the price of 0 -> 0
      total price -> 0
   */
  @ParameterizedTest
  @CsvSource({"25.0, 25.0, 500.0", "10.0, 5.0, 150.0", "0.0, 0.0, 0.0"})
  public void getMarketPrice_no_discount_success(double price1, double price2, double expected) {
    when(superMarket.getPrice(product1.productId)).thenReturn(price1);
    when(superMarket.getPrice(product2.productId)).thenReturn(price2);
    double totalPrice = shoppingList.getMarketPrice();
    verify(shoppingList).getDiscount(expected);
    assertEquals(expected, totalPrice, "10 apples for the price of " + price1 + ", and 10 pears for the price of " + price2 + " -> should give " + expected);
  }


  /*  Use Cases :
      10 apples for the price of 25 each -> 250
      10 pears for the price of 25.5 -> 255
      total price -> 505  ,   discount -> 5%
      price after discount -> 505 * 0.95 = 712.5

      10 apples for the price of 50 each -> 500
      10 pears for the price of 25 -> 250
      total price -> 750  ,   discount -> 5%
      price after discount -> 750 * 0.95 = 712.5

      10 apples for the price of 60 each -> 600
      10 pears for the price of 25 -> 250
      total price -> 850  ,   discount -> 10%
      price after discount -> 850 * 0.9 = 765

      10 apples for the price of 60 each -> 600
      10 pears for the price of 40 -> 400
      total price -> 1000  ,   discount -> 10%
      price after discount -> 1000 * 0.9 = 900

      10 apples for the price of 60 each -> 600
      10 pears for the price of 50 -> 500
      total price -> 1100  ,   discount -> 15%
      price after discount -> 1100 * 0.85 = 935
  */
  @ParameterizedTest
  @CsvSource({"25.0, 25.5, 505.0, 0.95, 479.75", "50.0, 25.0, 750.0, 0.05, 712.5", "60.0, 25.0, 850.0, 0.9, 765.0", "60.0, 40.0, 1000.0, 0.9, 900.0", "60.0, 50.0, 1100.0, 0.85, 935.0"})
  public void getMarketPrice_with_discount_success(double price1, double price2, double fullPrice, double discount, double expected) {
    when(superMarket.getPrice(product1.productId)).thenReturn(price1);
    when(superMarket.getPrice(product2.productId)).thenReturn(price2);
    double totalPrice = shoppingList.getMarketPrice();
    verify(shoppingList).getDiscount(fullPrice);
    assertEquals(expected, totalPrice, "10 apples for the price of " + price1 + ", and 10 pears for the price of " + price2 + " -> should give " + expected);
  }

  @Test
  public void changeQuantity_no_product_fail() {
    shoppingList.changeQuantity(5, "banana");
    Mockito.verify(product1, times(0)).setQuantity(anyInt());
    Mockito.verify(product2, times(0)).setQuantity(anyInt());
  }

  @ParameterizedTest
  @CsvSource({"-1, apple", "-300, pear"})
  public void changeQuantity_illegal_quantity_fail(int quantity, String id) {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> shoppingList.changeQuantity(quantity, id));
    assertEquals("Quantity cannot be negative", thrown.getMessage());
    Mockito.verifyZeroInteractions(product1);
    Mockito.verifyZeroInteractions(product2);
  }

  @Test
  public void changeQuantity_zero_success() throws Exception {
    shoppingList.changeQuantity(0, "apple");

    List<Product> products = getProducts();

    Mockito.verify(product1, times(0)).setQuantity(anyInt());
    Mockito.verify(product2, times(0)).setQuantity(anyInt());

    assertFalse(products.contains(product1), "Product1 should have been deleted");
    assertTrue(products.contains(product2), "Product2 shouldn't have been deleted");
  }

  @ParameterizedTest
  @ValueSource(ints = {50, 1, 100, Integer.MAX_VALUE})
  public void changeQuantity_success(int quantity) {
    shoppingList.changeQuantity(quantity, "apple");
    Mockito.verify(product1, times(1)).setQuantity(quantity);
    Mockito.verify(product2, times(0)).setQuantity(anyInt());
  }

  @ParameterizedTest
  @MethodSource("deliveryPositiveParams")
  void priceWithDelivery_positive_miles_success(int miles, double totalPrice, double deliveryPrice, double expected) {
    when(shoppingList.getMarketPrice()).thenReturn(totalPrice);
    when(superMarket.calcDeliveryFee(miles, 2)).thenReturn(deliveryPrice);
    double result = shoppingList.priceWithDelivery(miles);
    assertEquals(expected, result, "price -> " + expected + " = " + deliveryPrice + " + " + totalPrice);
  }

  private static Stream<Arguments> deliveryPositiveParams() {
    return Stream.of(
        Arguments.of(0, 200.0, 0.0, 200.0),
        Arguments.of(1, 200.0, 5.0, 205.0),
        Arguments.of(20, 200.0, 100.0, 300.0),
        Arguments.of(100, 200.0, 500.0, 700.0)
    );
  }

  @ParameterizedTest
  @MethodSource("deliveryNegativeParams")
  public void priceWithDelivery_negative_miles_fail(int miles, double totalPrice, double deliveryPrice, double expected) {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class , () -> shoppingList.priceWithDelivery(miles), "Delivery for " + miles + " should throw error");
    assertEquals("Miles cannot be negative", thrown.getMessage());
  }

  private static Stream<Arguments> deliveryNegativeParams() {
    return Stream.of(
            Arguments.of(-20, 200.0, 100.0, 300.0),
            Arguments.of(Integer.MIN_VALUE, 200.0, 100.0, 300.0),
            Arguments.of(-1, 200.0, 5.0, 205.0)
    );
  }

  /*
    price < 500 -> no discount ( = 1)
    500 <= price < 750 -> 5% discount ( = 0.95)
    750 <= price < 1000 -> 10% discount ( = 0.9)
    1000 <= price -> 15% discount ( = 0.85)
   */
  @ParameterizedTest
  @MethodSource("discountSuccessParams")
  public void getDiscount_success(double price, double expected){
    double discount = shoppingList.getDiscount(price);
    assertEquals(expected, discount, "Discount for " + price + " should be " + expected);
  }

  public static Stream<Arguments> discountSuccessParams(){
    return Stream.of(
            Arguments.of(0.0, 1.0),
            Arguments.of(500.0, 1.0),
            Arguments.of(501.0, 0.95),
            Arguments.of(750.0, 0.95),
            Arguments.of(751.0, 0.9),
            Arguments.of(1000.0, 0.9),
            Arguments.of(1001.0, 0.85),
            Arguments.of(Double.MAX_VALUE, 0.85)
    );
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.01, -1.0, Double.MAX_VALUE * -1})
  public void getDiscount_fail(double price){
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class , () -> shoppingList.getDiscount(price), "Discount for " + price + " should throw exception");
    assertEquals("Price cannot be negative", thrown.getMessage());
  }

  public List<Product> getProducts() throws Exception {
    Field products_field = ShoppingList.class.getDeclaredField("products");
    products_field.setAccessible(true);
    List<Product> products = (List<Product>)products_field.get(shoppingList);
    return products;
  }
}



