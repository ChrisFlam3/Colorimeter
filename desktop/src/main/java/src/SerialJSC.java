package src;

import com.fazecast.jSerialComm.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SerialJSC {
    SerialPort comPort;

    public void initialize() {
        this.comPort = SerialPort.getCommPort("COM3");
        this.comPort.setBaudRate(115200);
        this.comPort.openPort();
        this.comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
    }

    public synchronized void sendInitialMessage (int length) {

        comPort.writeBytes(new byte[]{116}, 1);

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
            //System.out.println("Response: " + new String(response, StandardCharsets.US_ASCII));
        }
    }
}
