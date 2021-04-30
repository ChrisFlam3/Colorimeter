package src;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            String path = System.getProperty("user.dir");

            List<Float> xys = TestController.colorsToXy(this.colorQueue);
            List<String> xysStrings = new ArrayList<>();
            xys.stream().map(x -> x.toString()).forEach(xysStrings::add);

            int length=colorQueue.size();
            main.sendInitialMessage(colorQueue.size());

            while (!colorQueue.isEmpty()) {
                Color color = colorQueue.remove(0);
                setColor(color);

                sendMessage(color);
            }

            List<Float> dxdys = main.receiveDifferences(length);
            List<String> dxdysStrings = new ArrayList<>();
            dxdys.stream().map(x -> x.toString()).forEach(xysStrings::add);

            List<String> commands = new ArrayList<>();
            commands.add("python");
            commands.add(path + "plot.py");
            commands.add(path);
            commands.addAll(xysStrings);
            commands.addAll(dxdysStrings);

            ProcessBuilder pb = new ProcessBuilder(commands);
            try {
                Process p = pb.start();
                p.waitFor(); // Wait for the process to finish.
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void setCanvasColor (Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawImage (String path) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Image image = new Image(path);
        gc.drawImage(image, 0, 0);
    }

    public static List<Float> colorsToXy (List<Color> colors) {
        List<Float> xys = new ArrayList<>();
        for (Color color: colors){
            double red = color.getRed();
            double green = color.getGreen();
            double blue = color.getBlue();

            double X = 0.4124 * red + 0.3576 * green + 0.1805 * blue;
            double Y = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
            double Z = 0.0193 * red + 0.1192 * green + 0.9505 * blue;

            float x = (float)(X / (X + Y + Z));
            float y = (float)(X / (X + Y + Z));

            xys.add(x);
            xys.add(y);
        }
        return xys;
    }

//    public static void
}
