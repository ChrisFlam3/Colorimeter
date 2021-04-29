package src;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage primaryStage;

    private AppController appController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("hello world");

        this.appController = new AppController(primaryStage);
        this.appController.initRootLayout();

    }
}
