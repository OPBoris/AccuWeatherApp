module fhtw.accuweatherapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens fhtw.accuweatherapp to javafx.fxml;
    exports fhtw.accuweatherapp;
}