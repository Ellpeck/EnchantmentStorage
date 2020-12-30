package de.ellpeck.enchantmentstorage;

import de.ellpeck.enchantmentstorage.network.PacketHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = EnchantmentStorage.ID, name = EnchantmentStorage.NAME, version = EnchantmentStorage.VERSION)
public class EnchantmentStorage {

    public static final String ID = "enchantmentstorage";
    public static final String NAME = "Enchantment Storage";
    public static final String VERSION = "@VERSION@";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.init(event.getSuggestedConfigurationFile());
        Registry.preInit();
        PacketHandler.preInit();
    }
}
