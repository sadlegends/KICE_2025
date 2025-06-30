package KICE_2025.pilot.SUB3;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SP_TEST {

    private static final String DICTIONARY_FILE = "DICTIONARY.TXT";
    private static final String STOPWORD_FILE = "STOPWORD.TXT";
    private static final String MODELS_FILE = "MODELS.JSON";

    private static Map<String, String> dictionary = new HashMap<>();
    private static Set<String> stopwords = new HashSet<>();
    private static ModelsConfig modelsConfig;
    private static Map<String, ModelInfo> modelMap = new HashMap<>();

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    static class ModelsConfig {
        List<ModelInfo> models;
    }

    static class ModelInfo {
        String modelname;
        List<ModelClass> classes;
        String url;
        Map<String, String> classCodeToValueMap;
    }

    static class ModelClass {
        String code;
        String value;
    }

    static class AiServiceRequest {
        String modelname;
        List<String> queries;
    }

    static class AiServiceResponse {
        List<String> results;
        public AiServiceResponse(List<String> results) { this.results = results; }
    }

    static class ExternalAiRequest {
        String query;
        public ExternalAiRequest(String query) { this.query = query; }
    }

    static class ExternalAiResponse {
        String result;
    }

    public static void loadDictionary() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(Paths.get(DICTIONARY_FILE).toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("#");
                if (parts.length == 2) {
                    dictionary.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }

    public static void loadStopwords() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(Paths.get(STOPWORD_FILE).toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopwords.add(line.trim());
            }
        }
    }

    public static void loadModelsConfig() throws IOException {
        String jsonContent = Files.readString(Paths.get(MODELS_FILE));
        modelsConfig = gson.fromJson(jsonContent, ModelsConfig.class);
        for (ModelInfo model : modelsConfig.models) {
            model.classCodeToValueMap = new HashMap<>();
            for (ModelClass mc : model.classes) {
                model.classCodeToValueMap.put(mc.code, mc.value);
            }
            modelMap.put(model.modelname, model);
        }
    }

    public static String preprocessSingleSentence(String sentence) {
        String[] words = sentence.split("\\s+");
        List<String> vectors = new ArrayList<>();
        for (String word : words) {
            String lowerCaseWord = word.toLowerCase();
            String vector = dictionary.get(lowerCaseWord);
            if (vector != null && !stopwords.contains(vector)) {
                vectors.add(vector);
            }
        }
        return vectors.stream().collect(Collectors.joining(" "));
    }

    public static class AiServiceHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            if (!request.getMethod().equalsIgnoreCase("POST") || !target.equals("/")) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                baseRequest.setHandled(true);
                return;
            }

            String requestBody = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            AiServiceRequest aiRequest = gson.fromJson(requestBody, AiServiceRequest.class);
            ModelInfo selectedModel = modelMap.get(aiRequest.modelname);

            if (selectedModel == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println("{\"error\":\"Model not found: " + aiRequest.modelname + "\"}");
                baseRequest.setHandled(true);
                return;
            }

            List<String> classificationResults = new ArrayList<>();
            for (String querySentence : aiRequest.queries) {
                String preprocessedSentence = preprocessSingleSentence(querySentence);

                ExternalAiRequest externalRequestPayload = new ExternalAiRequest(preprocessedSentence);
                String externalApiRequestBody = gson.toJson(externalRequestPayload);

                HttpRequest externalHttpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(selectedModel.url))
                        .POST(HttpRequest.BodyPublishers.ofString(externalApiRequestBody))
                        .header("Content-Type", "application/json")
                        .build();
                try {
                    HttpResponse<String> externalHttpResponse = httpClient.send(externalHttpRequest, HttpResponse.BodyHandlers.ofString());
                    if (externalHttpResponse.statusCode() == 200) {
                        ExternalAiResponse externalAiResponse = gson.fromJson(externalHttpResponse.body(), ExternalAiResponse.class);
                        String classificationCode = externalAiResponse.result;
                        String classificationValue = selectedModel.classCodeToValueMap.get(classificationCode);
                        if (classificationValue != null) {
                            classificationResults.add(classificationValue);
                        } else {
                            classificationResults.add("unknown_code_" + classificationCode);
                        }
                    } else {
                        System.err.println("External AI model error: " + externalHttpResponse.statusCode() + " " + externalHttpResponse.body());
                        classificationResults.add("error_calling_model");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("HTTP client request interrupted: " + e.getMessage());
                    classificationResults.add("error_interrupted");
                }  catch (IOException e) {
                    System.err.println("HTTP client IO error: " + e.getMessage());
                    classificationResults.add("error_io_calling_model");
                }
            }

            AiServiceResponse serviceResponse = new AiServiceResponse(classificationResults);
            String jsonResponse = gson.toJson(serviceResponse);

            response.setContentType("application/json; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(jsonResponse);
            baseRequest.setHandled(true);
        }
    }

    public static void main(String[] args) {
        try {
            loadDictionary();
            loadStopwords();
            loadModelsConfig();
        } catch (IOException e) {
            System.err.println("Fatal: Error loading initial data files: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Server server = new Server(8080);
        server.setHandler(new AiServiceHandler());

        try {
            server.start();
            System.out.println("SP_TEST server started on port 8080.");
            System.out.println("Press Ctrl+C to stop.");
            server.join();
        } catch (Exception e) {
            System.err.println("Error starting or running server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
