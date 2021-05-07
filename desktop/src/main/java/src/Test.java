package src;

public class Test {
    public static void main(String[] args){
        try {
            String str = System.getProperty("user.dir");
            String str2 = "\\src\\main\\python\\plot.py";
            String str3 = "\\src\\main\\resources\\plots";

            ProcessBuilder pb = new ProcessBuilder("python", str + str2, str + str3, "0.3", "0.3", "0.1", "0.1");
            Process p = pb.start(); // Start the process.
            p.waitFor(); // Wait for the process to finish.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
