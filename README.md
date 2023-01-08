# JUnit with Mockito
A possible solution for this assignment is in [ShoppingListTest.java](src/test/java/sise/sqe/ShoppingListTest.java).

Things to notice:
1. All classes are either mocked or spied. This helps to verify side effects. For example, that `getDiscount` for example, has been called with the correct amount during the call to `getMarketPrice`.
2. Side effects can be undesired as well. For example, the call to `changeQuantity_no_product_fail` should not change quantity of a product that does not exist, and it should also not change the quantity of other products in the list.
3. `ParameterizedTest` are used where possible. 
4. The correct naming conventions.
5. How do we test the effect of a function on a private field? For example, `addProduct`? There are several options for that:
   1. Change the field visibility and add annotation like [@VisibleForTesting](https://guava.dev/releases/19.0/api/docs/com/google/common/annotations/VisibleForTesting.html). This was not an option in this assignment because you were not allowed to change the signature but in real life you will be able to do that in many cases.
   2. Add a constructor/setter that gets a list of products. Again, this constructor can be marked with a [@VisibleForTesting](https://guava.dev/releases/19.0/api/docs/com/google/common/annotations/VisibleForTesting.html). This option is preferred over the first option because it does not reveal the internals, unless you choose to use this class in an "unsafe" manner, e.g., for testing. 
   3. Use reflection, like the solution in `getProducts`. This option is legit if we do not want to use the [@VisibleForTesting](https://guava.dev/releases/19.0/api/docs/com/google/common/annotations/VisibleForTesting.html) annotation, or if we cannot change the signature. The problem here is that the test is tightly coupled with the implementation and not with the desired behavior. Therefore, changes in the implementation will probably break the test (e.g., replacing the list with a map, or a set).