module com.example.rocnikovka {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.rocnikovka to javafx.graphics, javafx.fxml;

    exports com.example.rocnikovka;
}