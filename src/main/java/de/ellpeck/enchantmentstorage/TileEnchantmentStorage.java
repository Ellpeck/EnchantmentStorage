package de.ellpeck.enchantmentstorage;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TileEnchantmentStorage extends TileEntity implements ITickable {

    public static final int BOOK_IN_SLOT = 0;
    public static final int BOOK_OUT_SLOT = 1;
    public final ItemStackHandler items = new ItemStackHandler(2);
    public final Map<ResourceLocation, MutableInt> storedEnchantments = new HashMap<>();

    @Override
    public void update() {
        if (!this.world.isRemote) {
            // store enchantments
            ItemStack book = this.items.getStackInSlot(BOOK_IN_SLOT);
            if (!book.isEmpty() && book.getItem() == Items.ENCHANTED_BOOK) {
                for (Map.Entry<Enchantment, Integer> ench : EnchantmentHelper.getEnchantments(book).entrySet()) {
                    MutableInt current = this.storedEnchantments.computeIfAbsent(ench.getKey().getRegistryName(), e -> new MutableInt());
                    current.add(getLevelOneCount(ench.getValue()));
                }
                book.shrink(1);
                this.sendToClients();
                this.markDirty();
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("items", this.items.serializeNBT());
        NBTTagList list = new NBTTagList();
        for (Map.Entry<ResourceLocation, MutableInt> ench : this.storedEnchantments.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("enchantment", ench.getKey().toString());
            tag.setInteger("amount", ench.getValue().intValue());
            list.appendTag(tag);
        }
        compound.setTag("enchantments", list);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.items.deserializeNBT(compound.getCompoundTag("items"));
        this.storedEnchantments.clear();
        NBTTagList list = compound.getTagList("enchantments", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            this.storedEnchantments.put(
                    new ResourceLocation(tag.getString("enchantment")),
                    new MutableInt(tag.getInteger("amount")));
            System.out.println(tag.getString("enchantment"));
        }
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, -1, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    public void sendToClients() {
        WorldServer world = (WorldServer) this.getWorld();
        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(this.getPos().getX() >> 4, this.getPos().getZ() >> 4);
        if (entry != null)
            entry.sendPacket(this.getUpdatePacket());
    }

    public static int getLevelOneCount(int level) {
        return (int) Math.pow(2, level - 1);
    }
}
