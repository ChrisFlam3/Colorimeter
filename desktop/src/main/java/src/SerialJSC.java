package src;

import com.fazecast.jSerialComm.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SerialJSC {
    SerialPort comPort;

    public void initialize() {
        this.comPort = SerialPort.getCommPort("COM3");
        this.comPort.setBaudRate(115200);
        this.comPort.openPort();
        this.comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
    }

    public synchronized void sendInitialMessage (byte type, int length) {

        comPort.writeBytes(new byte[]{type}, 1);

        comPort.writeBytes(Integer.toString(length).getBytes(), Integer.toString(length).getBytes().length);
        byte[] response = new byte[1];
        response[0] = 0;
        while (response[0] != (byte)255) {
            int numRead = comPort.readBytes(response, 1);
            //System.out.println("Response: " + new String(response, StandardCharsets.US_ASCII));
        }
    }

    public synchronized void sendMessage (byte[] message) {
        comPort.writeBytes(message, message.length);

        byte[] response = new byte[1];
        response[0] = 0;
        while (response[0] != (byte)0xFF) {
            int numRead = comPort.readBytes(response, response.length);
            System.out.println("Response: " + new String(response, StandardCharsets.US_ASCII));
        }
    }

    public synchronized List<Float> receiveDifferences (int length) {
        List<Float> convertedList=new ArrayList<>();
        String result="";
        while (true) {
            int available = comPort.bytesAvailable();
            if(available<1)
                continue;
            byte[] response = new byte[available];
            int numRead = comPort.readBytes(response, available);
            if(numRead<1)
                continue;
            String responseString = new String(response, StandardCharsets.US_ASCII);
            result+=responseString;
            if(response[response.length-1]== (byte)0xFF)
                break;
        }
            System.out.println("Response: " +result);
            result = result.substring(0, result.length() - 1);

            convertedList.addAll(Stream.of(result.split(","))
                    .map(String::trim)
                    .map(Float::parseFloat)
                    .collect(Collectors.toList()));

        return convertedList;
    }

    public synchronized Float receiveFloat () {
        while (true) {
            int available = comPort.bytesAvailable();
            if(available<1)
                continue;
            byte[] response = new byte[available];
            int numRead = comPort.readBytes(response, available);
            if(numRead<1)
                continue;
            String responseString = new String(response, StandardCharsets.US_ASCII);

            return Float.parseFloat(responseString);
        }
    }

}
