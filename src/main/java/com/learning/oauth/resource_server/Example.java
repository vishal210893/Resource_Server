package com.learning.oauth.resource_server;

import java.util.function.Consumer;

class Calculator {
    // Method that RETURNS a value
    public int add(int value) {
        System.out.println("Adding " + value);
        return 10 + value;  // Returns a number
    }
}

public class Example {
    public static void main(String[] args) {
        // Calculator calc = new Calculator();

        // Consumer expects void, but add() returns int
        // The return value is IGNORED!
        Consumer<Calculator> adder = calc -> calc.add(5);

        Consumer<Calculator> add = new Consumer<Calculator>() {
            @Override
            public void accept(Calculator calculator) {
                calculator.add(5);
            }
        };

        Consumer<Calculator> add1 = new callimplem();


        adder.accept(new Calculator());  // Prints: Adding 5
        // The returned value (15) is discarded!
    }
}

class callimplem implements Consumer<Calculator> {

    @Override
    public void accept(Calculator calculator) {
        calculator.add(5);
    }
}
