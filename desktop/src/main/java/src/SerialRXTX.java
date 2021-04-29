package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;


public class SerialRXTX implements SerialPortEventListener {
    SerialPort serialPort;
    public String results = "";
    public boolean waiting = false;

    private static final String[] PORT_NAMES = {
            "/dev/tty.usbserial-A9007UX1", // Mac OS X
            "/dev/ttyACM0", // Raspberry Pi
            "/dev/ttyUSB0", // Linux
            "COM3", // Windows
    };

    private BufferedReader input;
    public OutputStream output;
    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 115200;

    public void initialize() {
        System.setProperty("gnu.io.rxtx.SerialPorts", "COM3");

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            for (String portName : PORT_NAMES) {
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }
        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }

        try {
            serialPort = (SerialPort) portId.open(this.getClass().getName(),
                    TIME_OUT);

            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public synchronized void sendInitialMessage(int length) {
        // initial message

        try {
            output.write(new byte[]{116});
            output.write(new byte[]{(byte) length});
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.waiting = true;
    }

    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                String inputLine = input.readLine();
                System.out.println(inputLine);
                if(Arrays.equals(inputLine.getBytes(StandardCharsets.US_ASCII), new byte[]{127})){
                    this.wakeUp();
                }
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }

    public synchronized void goToSleep() {
        try {
            while (this.waiting) {
                this.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void wakeUp() {
        this.waiting = false;
        this.notifyAll();
    }

    public synchronized void makeBed() {
        this.waiting = true;
    }
}