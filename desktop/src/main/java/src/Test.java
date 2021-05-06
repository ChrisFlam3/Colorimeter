package src;

import static java.awt.Toolkit.getDefaultToolkit;

public class Test {
    public static void main(String[] args){
        try {
            getDefaultToolkit().getScreenResolution();
            String str = System.getProperty("user.dir");
            String str2 = "\\src\\main\\java\\src\\plot.py";
            String str3 = "\\src\\main\\java\\src";
//            String[] script = {"python", str + str2, str + str3, "0.2", "0.2", "0.1", "0.1"};
//            Runtime.getRuntime().exec(script);
//            ProcessBuilder pb = new ProcessBuilder(script);
//            String str = System.getProperty("user.dir");

            ProcessBuilder pb = new ProcessBuilder("python", str + str2, str + str3, "0.3", "0.3", "0.1", "0.1");
            Process p = pb.start(); // Start the process.
            p.waitFor(); // Wait for the process to finish.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
