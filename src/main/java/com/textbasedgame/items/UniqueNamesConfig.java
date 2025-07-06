package com.textbasedgame.items;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UniqueNamesConfig {
    public record NamesByCategory(String name, String description){};

    private final Map<ItemsSubtypes, List<NamesByCategory>> namesByCategory;

    public UniqueNamesConfig()  {
        try (InputStream inputStream = UniqueNamesConfig.class.getClassLoader().getResourceAsStream("itemsdata/uniques.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<String, List<NamesByCategory>>> typeRef = new TypeReference<>() {};
            Map<String, List<NamesByCategory>> raw = objectMapper.readValue(inputStream, typeRef);
            Map<ItemsSubtypes, List<NamesByCategory>> typed = new EnumMap<>(ItemsSubtypes.class);
            if(raw.isEmpty() || raw.size() < ItemsSubtypes.values().length) {
                this.namesByCategory = createAndWriteDefaultTemplate(objectMapper);
                return;
            }

            for (ItemsSubtypes e : ItemsSubtypes.values()) {

                List<NamesByCategory> currentVal = raw.get(e.toString());

                if (currentVal == null || currentVal.isEmpty()) {
                    continue;
                }

                typed.put(e, currentVal);
            }
            this.namesByCategory = Collections.unmodifiableMap(typed);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }


    }

    public List<NamesByCategory> getNames(ItemsSubtypes category) {
        return namesByCategory.getOrDefault(category, List.of());
    }

    public NamesByCategory pickUniqueName(ItemsSubtypes category) {
        List<NamesByCategory> pool = this.getNames(category);
        if (pool.isEmpty()) { return new NamesByCategory("", "");}
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private Map<ItemsSubtypes, List<NamesByCategory>> createAndWriteDefaultTemplate(ObjectMapper mapper) throws IOException {
        Map<ItemsSubtypes, List<NamesByCategory>> defaults = new EnumMap<>(ItemsSubtypes.class);

        for (ItemsSubtypes subtype : ItemsSubtypes.values()) {
            defaults.put(subtype, List.of(new NamesByCategory("", "")));
        }

        Path outPath = Paths.get("src/main/resources/itemsdata/uniques.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(outPath.toFile(), defaults);
        return defaults;
    }
}
