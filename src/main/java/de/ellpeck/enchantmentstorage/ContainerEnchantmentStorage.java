package de.ellpeck.enchantmentstorage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;

public class ContainerEnchantmentStorage extends Container {

    private final TileEnchantmentStorage tile;

    public ContainerEnchantmentStorage(EntityPlayer player, TileEnchantmentStorage tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotItemHandler(tile.items, TileEnchantmentStorage.BOOK_IN_SLOT, 158, 12));
        this.addSlotToContainer(new SlotItemHandler(tile.items, TileEnchantmentStorage.BOOK_OUT_SLOT, 194, 45));

        // player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j)
                this.addSlotToContainer(new Slot(player.inventory, j + i * 9 + 9, 140 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k)
            this.addSlotToContainer(new Slot(player.inventory, k, 140 + k * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !this.tile.isInvalid();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return transferStackInSlot(this, this::mergeItemStack, playerIn, index, s -> {
            if (s.getItem() == Items.ENCHANTED_BOOK)
                return Pair.of(TileEnchantmentStorage.BOOK_IN_SLOT, TileEnchantmentStorage.BOOK_IN_SLOT + 1);
            return null;
        });
    }

    // stolen from https://github.com/Ellpeck/PrettyPipes/blob/main/src/main/java/de/ellpeck/prettypipes/Utility.java#L71-L112
    public static ItemStack transferStackInSlot(Container container, IMergeItemStack merge, EntityPlayer player, int slotIndex, Function<ItemStack, Pair<Integer, Integer>> predicate) {
        int inventoryStart = (int) container.inventorySlots.stream().filter(slot -> slot.inventory != player.inventory).count();
        int inventoryEnd = inventoryStart + 26;
        int hotbarStart = inventoryEnd + 1;
        int hotbarEnd = hotbarStart + 8;

        Slot slot = container.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack newStack = slot.getStack();
            ItemStack currentStack = newStack.copy();

            if (slotIndex >= inventoryStart) {
                // shift into this container here
                // mergeItemStack with the slots that newStack should go into
                // return an empty stack if mergeItemStack fails
                Pair<Integer, Integer> slots = predicate.apply(newStack);
                if (slots != null) {
                    if (!merge.mergeItemStack(newStack, slots.getLeft(), slots.getRight(), false))
                        return ItemStack.EMPTY;
                }
                // end custom code
                else if (slotIndex >= inventoryStart && slotIndex <= inventoryEnd) {
                    if (!merge.mergeItemStack(newStack, hotbarStart, hotbarEnd + 1, false))
                        return ItemStack.EMPTY;
                } else if (slotIndex >= inventoryEnd + 1 && slotIndex < hotbarEnd + 1 && !merge.mergeItemStack(newStack, inventoryStart, inventoryEnd + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!merge.mergeItemStack(newStack, inventoryStart, hotbarEnd + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (newStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (newStack.getCount() == currentStack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(player, newStack);
            return currentStack;
        }
        return ItemStack.EMPTY;
    }

    public interface IMergeItemStack {
        boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
    }
}
