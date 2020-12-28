package de.ellpeck.enchantmentstorage;

import de.ellpeck.enchantmentstorage.network.PacketExtractEnchantment;
import de.ellpeck.enchantmentstorage.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.Map;

public class GuiEnchantmentStorage extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(EnchantmentStorage.ID, "textures/gui/enchantment_storage.png");
    private final TileEnchantmentStorage tile;
    private final InventoryPlayer playerInventory;
    private int lastEnchantmentHash;
    private GuiButton levelPlusButton;
    private GuiButton levelMinusButton;
    private GuiButton okayButton;
    private Enchantment selectedEnchantment;
    private int selectedEnchantmentAvailable;
    private int level = 1;

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

        this.levelPlusButton = this.addButton(new PlusMinusButton(-1, this.guiLeft + 164, this.guiTop + 35, 10, 15, "", 0, 203));
        this.levelPlusButton.enabled = false;
        this.levelMinusButton = this.addButton(new PlusMinusButton(-2, this.guiLeft + 144, this.guiTop + 35, 10, 15, "", 0, 218));
        this.levelMinusButton.enabled = false;
        this.okayButton = this.addButton(new GuiButtonExt(-3, this.guiLeft + 144, this.guiTop + 55, 30, 15, I18n.format("gui.done")));
        this.okayButton.enabled = false;

        this.lastEnchantmentHash = 0;
        this.updateEnchantmentsIfDirty();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.levelPlusButton) {
            this.level++;
        } else if (button == this.levelMinusButton) {
            this.level--;
        } else if (button instanceof EnchantmentButton) {
            EnchantmentButton ench = ((EnchantmentButton) button);
            this.selectedEnchantment = Enchantment.getEnchantmentByLocation(ench.enchantment.toString());
            this.selectedEnchantmentAvailable = ench.amount;
            this.level = 1;
        } else if (button == this.okayButton) {
            PacketHandler.sendToServer(new PacketExtractEnchantment(this.tile.getPos(), this.selectedEnchantment.getRegistryName(), this.level));
            this.selectedEnchantment = null;
            this.selectedEnchantmentAvailable = 0;
            this.level = 1;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.updateEnchantmentsIfDirty();
        this.levelPlusButton.enabled = this.selectedEnchantment != null && this.level < this.selectedEnchantment.getMaxLevel() && this.selectedEnchantmentAvailable >= TileEnchantmentStorage.getLevelOneCount(this.level + 1);
        this.levelMinusButton.enabled = this.selectedEnchantment != null && this.level > 1;
        this.okayButton.enabled = this.selectedEnchantment != null && this.tile.items.getStackInSlot(TileEnchantmentStorage.BOOK_OUT_SLOT).isEmpty();
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
        this.fontRenderer.drawString(String.valueOf(this.level), 156, 39, 4210752);
    }

    private void updateEnchantmentsIfDirty() {
        Map<ResourceLocation, MutableInt> enchantments = this.tile.storedEnchantments;
        if (enchantments.hashCode() == this.lastEnchantmentHash)
            return;
        this.lastEnchantmentHash = enchantments.hashCode();
        this.buttonList.removeIf(b -> b instanceof EnchantmentButton);
        for (Map.Entry<ResourceLocation, MutableInt> ench : enchantments.entrySet())
            this.addButton(new EnchantmentButton(this.buttonList.size(), 0, 0, ench.getKey(), ench.getValue().intValue()));
        this.scrollButtons();
    }

    private void scrollButtons() {
        int y = 18;
        for (GuiButton button : this.buttonList) {
            if (!(button instanceof EnchantmentButton))
                continue;
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

    private static class PlusMinusButton extends GuiButton {

        private final int u;
        private final int v;

        public PlusMinusButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, int u, int v) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
            this.u = u;
            this.v = v;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                FontRenderer fontrenderer = mc.fontRenderer;
                // GuiButton edit: bind our texture
                mc.getTextureManager().bindTexture(BACKGROUND);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
                int i = this.getHoverState(this.hovered);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                // GuiButton edit: draw with custom u and v
                drawModalRectWithCustomSizedTexture(this.x, this.y, this.u + i * this.width, this.v, this.width, this.height, 512, 256);
                this.mouseDragged(mc, mouseX, mouseY);
                int j = 14737632;
                if (this.packedFGColour != 0) {
                    j = this.packedFGColour;
                } else if (!this.enabled) {
                    j = 10526880;
                } else if (this.hovered) {
                    j = 16777120;
                }
                this.drawCenteredString(fontrenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, j);
            }
        }
    }
}
