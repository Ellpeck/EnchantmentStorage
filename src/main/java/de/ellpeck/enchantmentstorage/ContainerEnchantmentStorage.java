package de.ellpeck.enchantmentstorage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerEnchantmentStorage extends Container {

    private final TileEnchantmentStorage tile;

    public ContainerEnchantmentStorage(EntityPlayer player, TileEnchantmentStorage tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotItemHandler(tile.items, TileEnchantmentStorage.BOOK_IN_SLOT, 126, 20));
        this.addSlotToContainer(new SlotItemHandler(tile.items, TileEnchantmentStorage.BOOK_OUT_SLOT, 126, 53));

        // player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j)
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 108 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k)
            this.addSlotToContainer(new Slot(player.inventory, k, 108 + k * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !this.tile.isInvalid();
    }
}
