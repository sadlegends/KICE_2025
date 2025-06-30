import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LogProcessor_P1 {

    private static final String EVENT_CODES_FILE = "exam1/EVENT_CODES.TXT";
    private static Map<String, String> eventCodes = new HashMap<>();

    public static void main(String[] args) {
        loadEventCodes();
        processLogInput();
    }

    private static void loadEventCodes() {
        try (BufferedReader br = new BufferedReader(new FileReader(EVENT_CODES_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts.length == 2) {
                    eventCodes.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading event codes file: " + e.getMessage());
        }
    }

    private static void processLogInput() {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String logLine;
            while ((logLine = consoleReader.readLine()) != null) {
                String[] parts = logLine.split(";");
                if (parts.length >= 2) {
                    String eventCode = parts[1];
                    String eventName = eventCodes.get(eventCode);
                    if (eventName != null) {
                        System.out.println(eventCode + " - " + eventName);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading console input: " + e.getMessage());
        }
    }
}