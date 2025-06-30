package KICE_2025.pilot.SUB2;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class SP_TEST {

    private static final String DICTIONARY_FILE = "DICTIONARY.TXT";
    private static final String STOPWORD_FILE = "STOPWORD.TXT";
    private static Map<String, String> dictionary = new HashMap<>();
    private static Set<String> stopwords = new HashSet<>();

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

    public static String preprocessSentence(String sentence) {
        String[] words = sentence.split("\\s+");
        List<String> vectors = new ArrayList<>();

        for (String word : words) {
            String lowerCaseWord = word.toLowerCase();
            String vector = dictionary.get(lowerCaseWord);
            if (vector != null) {
                if (!stopwords.contains(vector)) {
                    vectors.add(vector);
                }
            }
        }
        return vectors.stream().collect(Collectors.joining(" "));
    }

    public static void main(String[] args) {
        try {
            loadDictionary();
            loadStopwords();
        } catch (IOException e) {
            System.err.println("Error loading data files: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String inputSentence = scanner.nextLine();
            if (inputSentence.isEmpty()) {
                continue;
            }
            String processedSentence = preprocessSentence(inputSentence);
            System.out.println(processedSentence);
        }
        scanner.close();
    }
}
