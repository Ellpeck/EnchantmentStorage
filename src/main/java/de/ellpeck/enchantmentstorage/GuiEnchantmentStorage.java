package de.ellpeck.enchantmentstorage;

import de.ellpeck.enchantmentstorage.network.PacketExtractEnchantment;
import de.ellpeck.enchantmentstorage.network.PacketExtractExperience;
import de.ellpeck.enchantmentstorage.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Map;

public class GuiEnchantmentStorage extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(EnchantmentStorage.ID, "textures/gui/enchantment_storage.png");
    private static final int ENCHANTMENT_AMOUNT = 7;
    private final TileEnchantmentStorage tile;
    private final InventoryPlayer playerInventory;
    private int lastEnchantmentHash;
    private GuiButton levelPlusButton;
    private GuiButton levelMinusButton;
    private GuiButton okayButton;
    private GuiButton extractXpButton;
    private Enchantment selectedEnchantment;
    private int selectedEnchantmentAvailable;
    private int level = 1;
    private int scrollOffset;

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
        this.okayButton = this.addButton(new GuiButtonExt(-3, this.guiLeft + 141, this.guiTop + 55, 36, 15, I18n.format("info." + EnchantmentStorage.ID + ".apply")));
        this.okayButton.enabled = false;
        this.extractXpButton = this.addButton(new GuiButtonExt(-4, this.guiLeft + 251, this.guiTop + 44, 40, 18, I18n.format("info." + EnchantmentStorage.ID + ".extract")));
        this.extractXpButton.enabled = this.mc.player.experienceLevel > 0;

        this.updateEnchantments();
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
        } else if (button == this.extractXpButton) {
            int amount = isShiftKeyDown() ? Integer.MAX_VALUE : isCtrlKeyDown() ? 10 : 1;
            PacketHandler.sendToServer(new PacketExtractExperience(this.tile.getPos(), amount));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.tile.storedEnchantments.hashCode() != this.lastEnchantmentHash)
            this.updateEnchantments();
        this.levelPlusButton.enabled = this.selectedEnchantment != null && this.level < this.selectedEnchantment.getMaxLevel() && this.selectedEnchantmentAvailable >= TileEnchantmentStorage.getLevelOneCount(this.level + 1);
        this.levelMinusButton.enabled = this.selectedEnchantment != null && this.level > 1;
        this.okayButton.enabled = this.selectedEnchantment != null && this.tile.items.getStackInSlot(TileEnchantmentStorage.BOOK_OUT_SLOT).isEmpty() && this.tile.experience.experienceLevel >= TileEnchantmentStorage.getCombinationCost(this.selectedEnchantment, this.level);
        this.extractXpButton.enabled = this.mc.player.experienceLevel > 0;
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

        // draw the scroll bar
        if (this.getMaxScrollOffset() <= 0) {
            drawModalRectWithCustomSizedTexture(this.guiLeft + 126, this.guiTop + 18, 6, 176, 6, 27, 512, 256);
        } else {
            int yOffset = (int) ((140 - 27) * (this.scrollOffset / (float) this.getMaxScrollOffset()));
            drawModalRectWithCustomSizedTexture(this.guiLeft + 126, this.guiTop + 18 + yOffset, 0, 176, 6, 27, 512, 256);
        }

        this.renderExpBar(this.guiLeft + 189, this.guiTop + 11);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("info." + EnchantmentStorage.ID + ".enchantments"), 5, 8, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 140, 74, 4210752);
        this.fontRenderer.drawString(String.valueOf(this.level), 156, 39, 4210752);
        if (this.selectedEnchantment != null) {
            int cost = TileEnchantmentStorage.getCombinationCost(this.selectedEnchantment, this.level);
            this.fontRenderer.drawString(String.valueOf(cost), 206, 24, cost > this.tile.experience.experienceLevel ? 0xAA0000 : 4210752);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        // scroll using the mouse wheel
        int scroll = (int) Math.signum(Mouse.getEventDWheel());
        if (scroll != 0) {
            int newOffset = MathHelper.clamp(this.scrollOffset - scroll, 0, this.getMaxScrollOffset());
            if (newOffset != this.scrollOffset) {
                this.scrollOffset = newOffset;
                this.updateEnchantments();
            }
        }
    }

    private void updateEnchantments() {
        Map<ResourceLocation, MutableInt> enchantments = this.tile.storedEnchantments;
        this.lastEnchantmentHash = enchantments.hashCode();
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, 0, this.getMaxScrollOffset());
        this.buttonList.removeIf(b -> b instanceof EnchantmentButton);
        for (Map.Entry<ResourceLocation, MutableInt> ench : enchantments.entrySet())
            this.addButton(new EnchantmentButton(this.buttonList.size(), 0, 0, ench.getKey(), ench.getValue().intValue()));
        this.scrollButtons();
    }

    private void scrollButtons() {
        int y = 18;
        int buttons = 0;
        for (GuiButton button : this.buttonList) {
            if (!(button instanceof EnchantmentButton))
                continue;
            button.visible = buttons >= this.scrollOffset && buttons < this.scrollOffset + ENCHANTMENT_AMOUNT;
            buttons++;
            if (button.visible) {
                button.x = this.guiLeft + 5;
                button.y = this.guiTop + y;
                y += 20;
            }
        }
    }

    private int getMaxScrollOffset() {
        return Math.max(0, this.tile.storedEnchantments.size() - ENCHANTMENT_AMOUNT);
    }

    // edited GuiIngame copy
    private void renderExpBar(int x, int y) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        int i = this.tile.experience.xpBarCap();

        if (i > 0) {
            int k = (int) (this.tile.experience.experience * 102);
            int l = y + 3;
            drawModalRectWithCustomSizedTexture(x, l, 0, 166, 102, 5, 512, 256);

            if (k > 0) {
                drawModalRectWithCustomSizedTexture(x, l, 0, 166 + 5, k, 5, 512, 256);
            }
        }

        if (this.tile.experience.experienceLevel > 0) {
            String s = "" + this.tile.experience.experienceLevel;
            int i1 = x + (102 - this.fontRenderer.getStringWidth(s)) / 2;
            int j1 = y - 31 - 4 + 32;
            this.fontRenderer.drawString(s, i1 + 1, j1, 0);
            this.fontRenderer.drawString(s, i1 - 1, j1, 0);
            this.fontRenderer.drawString(s, i1, j1 + 1, 0);
            this.fontRenderer.drawString(s, i1, j1 - 1, 0);
            this.fontRenderer.drawString(s, i1, j1, 8453920);
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
