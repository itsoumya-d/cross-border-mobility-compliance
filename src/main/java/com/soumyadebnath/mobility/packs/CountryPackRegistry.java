package com.soumyadebnath.mobility.packs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class CountryPackRegistry {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, List<CountryRule>> packs = new HashMap<>();

    public CountryPackRegistry() {
        loadPack("SG");
    }

    public synchronized void loadPack(String countryCode) {
        String code = countryCode.toUpperCase();
        try {
            JsonNode root = null;
            try (InputStream is = getClass().getResourceAsStream("/packs/" + code.toLowerCase() + "/rules.json")) {
                if (is != null) {
                    root = mapper.readTree(is);
                }
            }
            if (root == null) {
                Path p = Paths.get("packs", code.toLowerCase(), "rules.json");
                if (Files.exists(p)) {
                    root = mapper.readTree(p.toFile());
                }
            }
            if (root == null) {
                throw new IllegalArgumentException("Unknown country pack: " + countryCode + ". No rules file found under packs/" + code.toLowerCase() + "/rules.json");
            }
            List<CountryRule> rules = new ArrayList<>();
            JsonNode rulesArray = root.get("rules");
            if (rulesArray != null && rulesArray.isArray()) {
                for (JsonNode n : rulesArray) {
                    rules.add(new CountryRule(
                        n.path("id").asText(),
                        n.path("decision").asText(),
                        n.path("metric").asText(),
                        n.path("threshold_value").asDouble(),
                        n.path("category").asText(),
                        n.path("source_url").asText(),
                        n.path("checked_on").asText(),
                        n.path("description").asText()
                    ));
                }
            }
            packs.put(code, Collections.unmodifiableList(rules));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load country pack for " + countryCode + ": " + e.getMessage(), e);
        }
    }

    public List<CountryRule> getRules(String countryCode) {
        if (countryCode == null || !packs.containsKey(countryCode.toUpperCase())) {
            throw new IllegalArgumentException("Unsupported country pack: " + countryCode + ". Available packs: " + packs.keySet());
        }
        return packs.get(countryCode.toUpperCase());
    }

    public boolean isSupported(String countryCode) {
        return countryCode != null && packs.containsKey(countryCode.toUpperCase());
    }

    public Set<String> getSupportedCountries() {
        return Collections.unmodifiableSet(packs.keySet());
    }
}
