import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SP_TEST {

    private static final String DICTIONARY_FILE = "DICTIONARY.TXT";
    private static Map<String, String> dictionary = new HashMap<>();

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

    public static String preprocessSentence(String sentence) {
        String[] words = sentence.split("\\s+");
        List<String> vectors = new ArrayList<>();

        for (String word : words) {
            String lowerCaseWord = word.toLowerCase();
            String vector = dictionary.get(lowerCaseWord);
            if (vector != null) {
                vectors.add(vector);
            }
        }
        return vectors.stream().collect(Collectors.joining(" "));
    }

    public static void main(String[] args) {
        try {
            loadDictionary();
        } catch (IOException e) {
            System.err.println("Error loading dictionary: " + e.getMessage());
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
