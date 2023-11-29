package uk.ac.ed.inf;

import org.junit.jupiter.api.Test;

public class MainTest {
    String baseUrl = "https://ilp-rest.azurewebsites.net";
    String date = "2023-11-27";
    String[] args = {date, baseUrl};

    @Test
    void mainNormal(){
        Main.main(args);
    }
}
