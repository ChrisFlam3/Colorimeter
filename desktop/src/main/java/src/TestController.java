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
    private boolean isStateTesting;

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

    private SerialJSC main;

    public void setAppController(AppController appController) {
        this.appController = appController;
        if (!this.isStateTesting) {
            drawImage("file:///" + System.getProperty("user.dir") + "\\src\\main\\resources\\plots");
        }

        this.appController.primaryStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (this.appController.primaryStage.getHeight() > 0) {
                this.canvas.setHeight(this.appController.primaryStage.getHeight() - 50);
                this.canvas.setWidth(this.appController.primaryStage.getHeight() - 50);
            }
            if (!this.isStateTesting) {
                drawImage("file:///" + System.getProperty("user.dir") + "\\src\\main\\resources\\plots");
            }
        });
    }

    @FXML
    private void initialize() {
        this.isStateTesting = true;
        this.colorQueue = FXCollections.observableArrayList();

        new Thread(this::initializeSerial).start();
    }

    private void initializeSerial() {
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
    }

    @FXML
    private void handleExecuteAction() {
        new Thread(() -> {
            if (!colorQueue.isEmpty()) {
                this.isStateTesting = true;
                String pythonPath = System.getProperty("user.dir") + "\\src\\main\\python";
                String plotPath = System.getProperty("user.dir") + "\\src\\main\\resources\\plots";

                List<Float> xys = TestController.colorsToXy(this.colorQueue);
                List<String> xysStrings = new ArrayList<>();
                xys.stream().map(x -> x.toString()).forEach(xysStrings::add);

                int length=colorQueue.size();
                main.sendInitialMessage(colorQueue.size());

                while (!colorQueue.isEmpty()) {
                    Color color = colorQueue.remove(0);
                    double[] rgb={color.getRed(),color.getGreen(),color.getBlue()};
                    for(int i=0;i<3;i++){
                        if(rgb[i]<=0.0031308)
                            rgb[i]=12.92*rgb[i];
                        else
                            rgb[i]=(1.055*Math.pow(rgb[i],1/2.2)-0.055);
                    }
                    Color corrected=Color.rgb((int)(rgb[0]*255),(int)(rgb[1]*255),(int)(rgb[2]*255));
                    setColor(corrected);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendMessage(color);
                }

                List<Float> dxdys = main.receiveDifferences(length);
                List<String> dxdysStrings = new ArrayList<>();
                dxdys.stream().map(x -> x.toString()).forEach(xysStrings::add);

                List<String> commands = new ArrayList<>();
                commands.add("python");
                commands.add(pythonPath + "\\plot.py");
                commands.add(plotPath);
                commands.addAll(xysStrings);
                commands.addAll(dxdysStrings);

                ProcessBuilder pb = new ProcessBuilder(commands);
                try {
                    Process p = pb.start();
                    p.waitFor(); // Wait for the process to finish.
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                drawImage("file:///" + plotPath);
                this.isStateTesting = false;
        }}).start();
    }

    private void setCanvasColor (Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawImage (String path) {
        double height = canvas.getHeight();
        double width = canvas.getWidth();
        String filename = "";
        int inches = 5;
        while(inches < 23) {
            if (height < (inches + 0.5) * 96){
                filename = "\\plot" + Integer.toString(inches) + ".png";
                break;
            }
            inches += 6;
        }
        if (inches >= 23) {
            filename = "\\plot" + Integer.toString(inches) + ".png";
        }
        setCanvasColor(Color.WHITE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Image image = new Image(path + filename, width, height, false, true);
        gc.drawImage(image, 0, 0);
    }

    public static List<Float> colorsToXy (List<Color> colors) {
        List<Float> xys = new ArrayList<>();
        for (Color color: colors){
            double[] rgb=new double[3];
            rgb[0] = color.getRed();
            rgb[1] = color.getGreen();
            rgb[2] = color.getBlue();


            double X = 0.4124 * rgb[0] + 0.3576 *  rgb[1] + 0.1805 * rgb[2];
            double Y = 0.2126 * rgb[0] + 0.7152 *  rgb[1] + 0.0722 * rgb[2];
            double Z = 0.0193 * rgb[0] + 0.1192 *  rgb[1] + 0.9505 * rgb[2];
            
           // double X=0.4887180*rgb[0]+  0.3106803*rgb[1]+  0.2006017*rgb[2];
           // double Y=0.1762044*rgb[0]+  0.8129847*rgb[1]+  0.0108109*rgb[2];
           // double Z=0.0000000*rgb[0]+  0.0102048*rgb[1]+  0.9897952*rgb[2];

            float x = (float)(X / (X + Y + Z));
            float y = (float)(Y / (X + Y + Z));

            xys.add(x);
            xys.add(y);
        }
        return xys;
    }
}
