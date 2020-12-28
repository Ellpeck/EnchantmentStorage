package de.ellpeck.enchantmentstorage;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

public class GuiEnchantmentStorage extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(EnchantmentStorage.ID, "textures/gui/enchantment_storage.png");
    private final TileEnchantmentStorage tile;
    private final InventoryPlayer playerInventory;

    public GuiEnchantmentStorage(EntityPlayer player, TileEnchantmentStorage tile) {
        super(new ContainerEnchantmentStorage(player, tile));
        this.tile = tile;
        this.playerInventory = player.inventory;
        this.xSize = 276;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        drawModalRectWithCustomSizedTexture(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize, 512, 256);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("info." + EnchantmentStorage.ID + ".enchantments"), 5, 8, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 108, 74, 4210752);
    }

}
