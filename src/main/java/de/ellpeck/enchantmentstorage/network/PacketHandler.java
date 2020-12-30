package de.ellpeck.enchantmentstorage.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import de.ellpeck.enchantmentstorage.*;
import net.minecraftforge.fml.relauncher.Side;

public final class PacketHandler {
    private static SimpleNetworkWrapper network;

    public static void preInit() {
        network = new SimpleNetworkWrapper(EnchantmentStorage.ID);
        network.registerMessage(PacketExtractEnchantment.Handler.class, PacketExtractEnchantment.class, 0, Side.SERVER);
        network.registerMessage(PacketExtractExperience.Handler.class, PacketExtractExperience.class, 1, Side.SERVER);
    }

    public static void sendToServer(IMessage message) {
        network.sendToServer(message);
    }
}
