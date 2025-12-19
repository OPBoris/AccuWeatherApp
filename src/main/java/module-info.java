module fhtw.accuweatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens fhtw.accuweatherapp to javafx.fxml;
    exports fhtw.accuweatherapp;
}