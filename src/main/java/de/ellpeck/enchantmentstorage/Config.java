package de.ellpeck.enchantmentstorage;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class Config {
    private static Configuration instance;
    public static Map<String, Float> xpFluids;
    public static Map<ResourceLocation, Float> xpItems;
    public static float twerkXp;

    public static void init(File file) {
        instance = new Configuration(file);
        instance.load();
        load();
    }

    public static void load() {
        String[] fluids = instance.getStringList("general", "xpFluids", new String[]{"essence->1", "experience->1", "xpjuice->0.05"}, "The names of compatible xp fluids and the amount of experience points that one mb of it gives. Each entry should be formatted as fluidName->amount.");
        xpFluids = Arrays.stream(fluids).map(s -> s.split("->")).collect(Collectors.toMap(s -> s[0], s -> Float.parseFloat(s[1])));
        String[] items = instance.getStringList("general", "xpItems", new String[]{"minecraft:experience_bottle->10", "actuallyadditions:item_solidified_experience->8"}, "The registry names of items and the amount of experience points that one of them gives. Each entry should be formatted as domain:name->amount.");
        xpItems = Arrays.stream(items).map(s -> s.split("->")).collect(Collectors.toMap(s -> new ResourceLocation(s[0]), s -> Float.parseFloat(s[1])));
        twerkXp = instance.getFloat("general", "twerkXp", 0.05F, 0, 1000, "The amount of experience the enchantment storage receives when twerking close to it. Set to 0 to disable.");

        if (instance.hasChanged())
            instance.save();
    }
}
