package de.ellpeck.enchantmentstorage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class ContainerEnchantmentStorage extends Container {

    private final TileEnchantmentStorage tile;

    public ContainerEnchantmentStorage(EntityPlayer player, TileEnchantmentStorage tile) {
        this.tile = tile;

        // player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j)
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k)
            this.addSlotToContainer(new Slot(player.inventory, k, 8 + k * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !this.tile.isInvalid();
    }
}
