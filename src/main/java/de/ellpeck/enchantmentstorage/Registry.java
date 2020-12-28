package de.ellpeck.enchantmentstorage;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public final class Registry {

    public static Block enchantmentStorage;

    public static void preInit() {
        NetworkRegistry.INSTANCE.registerGuiHandler(EnchantmentStorage.ID, new GuiHandler());
    }

    @SubscribeEvent
    public static void onBlockRegistry(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(enchantmentStorage = new BlockEnchantmentStorage());
    }

    @SubscribeEvent
    public static void onItemRegistry(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(enchantmentStorage).setRegistryName(enchantmentStorage.getRegistryName()));
    }

    @Mod.EventBusSubscriber(Side.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onModelRegistry(ModelRegistryEvent event) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(enchantmentStorage), 0, new ModelResourceLocation(enchantmentStorage.getRegistryName(), "inventory"));
        }
    }

    public static class GuiHandler implements IGuiHandler {

        @Nullable
        @Override
        public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            return new ContainerEnchantmentStorage(player, (TileEnchantmentStorage) world.getTileEntity(new BlockPos(x, y, z)));
        }

        @Nullable
        @Override
        public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            return new GuiEnchantmentStorage(player, (TileEnchantmentStorage) world.getTileEntity(new BlockPos(x, y, z)));
        }
    }
}
