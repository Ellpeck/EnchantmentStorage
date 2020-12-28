package de.ellpeck.enchantmentstorage;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Map;

public class GuiEnchantmentStorage extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(EnchantmentStorage.ID, "textures/gui/enchantment_storage.png");
    private final TileEnchantmentStorage tile;
    private final InventoryPlayer playerInventory;
    private int lastEnchantmentHash;

    public GuiEnchantmentStorage(EntityPlayer player, TileEnchantmentStorage tile) {
        super(new ContainerEnchantmentStorage(player, tile));
        this.tile = tile;
        this.playerInventory = player.inventory;
        this.xSize = 308;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.lastEnchantmentHash = 0;
        this.updateEnchantmentsIfDirty();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.updateEnchantmentsIfDirty();
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
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 140, 74, 4210752);
    }

    private void updateEnchantmentsIfDirty() {
        Map<ResourceLocation, MutableInt> enchantments = this.tile.storedEnchantments;
        if (enchantments.hashCode() == this.lastEnchantmentHash)
            return;
        this.lastEnchantmentHash = enchantments.hashCode();
        this.buttonList.clear();
        for (Map.Entry<ResourceLocation, MutableInt> ench : enchantments.entrySet())
            this.addButton(new EnchantmentButton(this.buttonList.size(), 0, 0, ench.getKey(), ench.getValue().intValue()));
        this.scrollButtons();
    }

    private void scrollButtons() {
        int y = 18;
        for (GuiButton button : this.buttonList) {
            button.x = this.guiLeft + 5;
            button.y = this.guiTop + y;
            y += 20;
        }
    }

    private static class EnchantmentButton extends GuiButton {

        public final ResourceLocation enchantment;
        public final int amount;

        public EnchantmentButton(int buttonId, int x, int y, ResourceLocation enchantment, int amount) {
            super(buttonId, x, y, 120, 20, I18n.format(Enchantment.getEnchantmentByLocation(enchantment.toString()).getName()) + " x" + amount);
            this.enchantment = enchantment;
            this.amount = amount;
        }
    }
}
