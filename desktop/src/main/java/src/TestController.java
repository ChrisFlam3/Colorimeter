package src;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.io.IOException;

public class TestController {
    @FXML
    public Spinner<Integer> timeSpinner;

    private AppController appController;

    private ObservableList<Color> colorQueue;

    @FXML
    private TextField rText;

    @FXML
    private TextField gText;

    @FXML
    private TextField bText;

    @FXML
    private Canvas canvas;

    @FXML
    private Label scorePlayer;

//    private SerialRXTX main;
    private SerialJSC main;

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        colorQueue = FXCollections.observableArrayList();
        setCanvasColor(Color.WHITE);
        new Thread(this::initializeSerial).start();
    }

    private void initializeSerial() {
//        main = new SerialRXTX();
        main = new SerialJSC();
        main.initialize();
    }

    @FXML
    private void handleRgbAction(ActionEvent event) {
        try {
            Color color = Color.rgb(Integer.parseInt(rText.getText()), Integer.parseInt(gText.getText()), Integer.parseInt(bText.getText()));
            colorQueue.add(color);
            scorePlayer.setText(String.valueOf(colorQueue.size()));
        } catch (IllegalArgumentException ignored) {}
    }

    @FXML
    public void handleRandomAction(ActionEvent event) {
        for(int i = 0; i < timeSpinner.getValue(); i++){
            colorQueue.add(Color.rgb((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
        }
        scorePlayer.setText(String.valueOf(colorQueue.size()));
    }

    private void setColor(Color color) {
        Platform.runLater(() -> {
            setCanvasColor(color);
            scorePlayer.setText(String.valueOf(colorQueue.size()));
        });
    }

    private void sendMessage(Color color) {
        byte red = (byte) (color.getRed()*255);
        byte green = (byte) (color.getGreen()*255);
        byte blue = (byte) (color.getBlue()*255);
        byte[] message = {red, green, blue};

        main.sendMessage(message);
//        main.goToSleep();
//
//        try {
//            main.output.write(message);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        main.makeBed();
    }

    @FXML
    private void handleExecuteAction() {
        new Thread(() -> {
            int length=colorQueue.size();
            main.sendInitialMessage(colorQueue.size());

            while (!colorQueue.isEmpty()) {
                Color color = colorQueue.remove(0);
                setColor(color);

                sendMessage(color);
            }
            main.receiveDifferences(length);
            // end tests
//            while(main.waiting){
//                try{
//                    wait();
//                } catch(InterruptedException ignored){}
//            }

        }).start();
    }

    private void setCanvasColor (Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
