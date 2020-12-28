package de.ellpeck.enchantmentstorage;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class GuiEnchantmentStorage extends GuiContainer {

    private final TileEnchantmentStorage tile;

    public GuiEnchantmentStorage(EntityPlayer player, TileEnchantmentStorage tile) {
        super(new ContainerEnchantmentStorage(player, tile));
        this.tile = tile;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }
}
