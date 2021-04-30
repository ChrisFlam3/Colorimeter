package src;

public class Test {
    public static void main(String[] args){
        try {
//            String[] script = {"python", "C:\\Users\\nuttard\\repos\\Colorimeter\\desktop\\src\\main\\java\\src\\plot.py", "0.2", "0.2", "0.1", "0.1"};
//            Runtime.getRuntime().exec(script);
//            ProcessBuilder pb = new ProcessBuilder(script);
            ProcessBuilder pb = new ProcessBuilder("python", "C:\\Users\\nuttard\\repos\\Colorimeter\\desktop\\src\\main\\java\\src\\plot.py", "0.3", "0.3", "0.1", "0.1");
            Process p = pb.start(); // Start the process.
            p.waitFor(); // Wait for the process to finish.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
