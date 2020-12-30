package de.ellpeck.enchantmentstorage;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class Config {
    private static Configuration instance;
    public static Map<String, Float> xpFluids;

    public static void init(File file) {
        instance = new Configuration(file);
        instance.load();
        load();
    }

    public static void load() {
        String[] fluids = instance.getStringList("general", "xpFluids", new String[]{"essence->1", "experience->1", "xpjuice->0.05"}, "The names of compatible xp fluids and the amount of experience points that one mb of it gives. Each entry should be formatted as fluidName->amount.");
        xpFluids = Arrays.stream(fluids).map(s -> s.split("->")).collect(Collectors.toMap(s -> s[0], s -> Float.parseFloat(s[1])));

        if (instance.hasChanged())
            instance.save();
    }
}
