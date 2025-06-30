import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogProcessor_P3 {

    private static final String EVENT_CODES_FILE = "exam1/EVENT_CODES.TXT";
    private static final String MONITORED_USERS_FILE = "exam1/MONITORED_USERS.TXT";
    private static final int PORT = 8088;

    private static Map<String, EventDefinition> eventDefinitions = new HashMap<>();
    private static Set<Integer> monitoredUsers = new HashSet<>();

    private static List<CriticalEvent> criticalEvents = new CopyOnWriteArrayList<>(); // Thread-safe list
    private static Map<Integer, Integer> criticalEventsByUser = new ConcurrentHashMap<>(); // Thread-safe map

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        loadEventDefinitions();
        loadMonitoredUsers();

        Server server = new Server(PORT);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new EventHandler()), "/event");
        context.addServlet(new ServletHolder(new SummaryHandler()), "/summary");

        server.start();
        server.join();
    }

    private static void loadEventDefinitions() {
        try (BufferedReader br = new BufferedReader(new FileReader(EVENT_CODES_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts.length == 3) {
                    String code = parts[0];
                    String name = parts[1];
                    int criticality = Integer.parseInt(parts[2]);
                    eventDefinitions.put(code, new EventDefinition(code, name, criticality));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading event definitions: " + e.getMessage());
        }
    }

    private static void loadMonitoredUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(MONITORED_USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
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

    private static class EventDefinition {
        String code;
        String name;
        int criticality;

        EventDefinition(String code, String name, int criticality) {
            this.code = code;
            this.name = name;
            this.criticality = criticality;
        }
    }

    private static class CriticalEvent {
        long timestamp;
        int userId;
        String eventCode;
        String eventName;

        CriticalEvent(long timestamp, int userId, String eventCode, String eventName) {
            this.timestamp = timestamp;
            this.userId = userId;
            this.eventCode = eventCode;
            this.eventName = eventName;
        }
    }

    private static class EventHandler extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);

            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JsonObject requestBody = gson.fromJson(sb.toString(), JsonObject.class);

            boolean criticalEventLogged = false;
            if (requestBody != null && requestBody.has("log_line")) {
                String logLine = requestBody.get("log_line").getAsString();
                String[] parts = logLine.split(";");

                if (parts.length == 4) {
                    try {
                        long timestamp = Long.parseLong(parts[0]);
                        String eventCode = parts[1];
                        int userId = Integer.parseInt(parts[2]);
                        // String data = parts[3]; // data is not used in this problem for processing

                        EventDefinition eventDef = eventDefinitions.get(eventCode);

                        if (eventDef != null && eventDef.criticality == 1 && monitoredUsers.contains(userId)) {
                            CriticalEvent criticalEvent = new CriticalEvent(timestamp, userId, eventCode, eventDef.name);
                            criticalEvents.add(criticalEvent);
                            criticalEventsByUser.merge(userId, 1, Integer::sum); // Increment count or add 1
                            criticalEventLogged = true;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing log line: " + logLine + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Malformed log line received: " + logLine);
                }
            }

            JsonObject responseBody = new JsonObject();
            responseBody.addProperty("status", "recorded");
            responseBody.addProperty("critical_event_logged", criticalEventLogged);

            response.getWriter().println(gson.toJson(responseBody));
        }
    }

    private static class SummaryHandler extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);

            JsonObject summary = new JsonObject();
            summary.addProperty("total_critical_events", criticalEvents.size());

            JsonObject eventsByUserJson = new JsonObject();
            criticalEventsByUser.forEach((userId, count) -> eventsByUserJson.addProperty(String.valueOf(userId), count));
            summary.add("critical_events_by_user", eventsByUserJson);

            JsonObject lastEventDetails = new JsonObject();
            if (!criticalEvents.isEmpty()) {
                CriticalEvent lastEvent = criticalEvents.get(criticalEvents.size() - 1);
                lastEventDetails.addProperty("timestamp", lastEvent.timestamp);
                lastEventDetails.addProperty("user_id", String.valueOf(lastEvent.userId));
                lastEventDetails.addProperty("event_code", lastEvent.eventCode);
                lastEventDetails.addProperty("event_name", lastEvent.eventName);
            } else {
                lastEventDetails.addProperty("timestamp", (Long) null);
                lastEventDetails.addProperty("user_id", (String) null);
                lastEventDetails.addProperty("event_code", (String) null);
                lastEventDetails.addProperty("event_name", (String) null);
            }
            summary.add("last_critical_event_details", lastEventDetails);

            response.getWriter().println(gson.toJson(summary));
        }
    }
}