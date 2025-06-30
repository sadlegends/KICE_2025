import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LogProcessor_P2 {

    private static final String EVENT_CODES_FILE = "exam1/EVENT_CODES.TXT";
    private static final String MONITORED_USERS_FILE = "exam1/MONITORED_USERS.TXT";

    private static class EventData {
        String name;
        int criticality;

        EventData(String name, int criticality) {
            this.name = name;
            this.criticality = criticality;
        }
    }

    private static Map<String, EventData> eventCodes = new HashMap<>();
    private static Set<Integer> monitoredUsers = new HashSet<>();

    public static void main(String[] args) {
        loadEventCodes();
        loadMonitoredUsers();

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String logLine;

        try {
            while ((logLine = consoleReader.readLine()) != null) {
                processLogLine(logLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadEventCodes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(EVENT_CODES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts.length == 3) {
                    String code = parts[0];
                    String name = parts[1];
                    int criticality = Integer.parseInt(parts[2]);
                    eventCodes.put(code, new EventData(name, criticality));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading event codes: " + e.getMessage());
        }
    }

    private static void loadMonitoredUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(MONITORED_USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    monitoredUsers.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid user ID in monitored users file: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading monitored users: " + e.getMessage());
        }
    }

    private static void processLogLine(String logLine) {
        String[] parts = logLine.split(";");
        if (parts.length == 4) {
            try {
                // long timestamp = Long.parseLong(parts[0]); // Not used in this problem's output
                String eventCode = parts[1];
                int userId = Integer.parseInt(parts[2]);
                // String data = parts[3]; // Not used in this problem's output

                EventData eventData = eventCodes.get(eventCode);

                if (eventData != null && eventData.criticality == 1 && monitoredUsers.contains(userId)) {
                    System.out.printf("CRITICAL EVENT: User %d - Event %s (%s)%n", userId, eventCode, eventData.name);
                }

            } catch (NumberFormatException e) {
                // Ignore lines with invalid number formats
            }
        }
    }
}