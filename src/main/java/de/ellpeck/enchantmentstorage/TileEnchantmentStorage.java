package de.ellpeck.enchantmentstorage;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TileEnchantmentStorage extends TileEntity implements ITickable {

    public static final int BOOK_IN_SLOT = 0;
    public static final int BOOK_OUT_SLOT = 1;
    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            // not sure why this doesn't happen by default but ok
            if (!this.isItemValid(slot, stack))
                return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == BOOK_IN_SLOT)
                return stack.getItem() == Items.ENCHANTED_BOOK;
            return false;
        }
    };
    public final FluidTank tank = new FluidTank(Fluid.BUCKET_VOLUME) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && Config.xpFluids.containsKey(fluid.getFluid().getName());
        }

        @Override
        public boolean canDrain() {
            return false;
        }
    };
    public final ExperienceStorage experience = new ExperienceStorage();
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

            // convert tank contents to stored xp
            if (this.tank.getFluidAmount() > 0) {
                float added = Config.xpFluids.getOrDefault(this.tank.getFluid().getFluid().getName(), 0F);
                if (added > 0) {
                    this.tank.drainInternal(Integer.MAX_VALUE, true);
                    this.experience.addExperience(added);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("items", this.items.serializeNBT());
        compound.setTag("tank", this.tank.writeToNBT(new NBTTagCompound()));
        compound.setTag("xp", this.experience.serializeNBT());
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
        this.tank.readFromNBT(compound.getCompoundTag("tank"));
        this.experience.deserializeNBT(compound.getCompoundTag("xp"));
        this.storedEnchantments.clear();
        NBTTagList list = compound.getTagList("enchantments", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            this.storedEnchantments.put(
                    new ResourceLocation(tag.getString("enchantment")),
                    new MutableInt(tag.getInteger("amount")));
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

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return (T) this.items;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return (T) this.tank;
        return null;
    }

    public void createEnchantedBook(ResourceLocation enchantment, int level) {
        if (!this.items.getStackInSlot(BOOK_OUT_SLOT).isEmpty())
            return;
        MutableInt available = this.storedEnchantments.get(enchantment);
        if (available == null || available.intValue() <= 0)
            return;
        if (level <= 0 || available.intValue() < getLevelOneCount(level))
            return;
        ItemStack ret = new ItemStack(Items.ENCHANTED_BOOK);
        Enchantment ench = Enchantment.getEnchantmentByLocation(enchantment.toString());
        ItemEnchantedBook.addEnchantment(ret, new EnchantmentData(ench, level));
        this.items.setStackInSlot(BOOK_OUT_SLOT, ret);

        available.subtract(getLevelOneCount(level));
        if (available.intValue() <= 0)
            this.storedEnchantments.remove(enchantment);
        this.sendToClients();
        this.markDirty();
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

    // slightly modified copy of EntityPlayer content
    public static class ExperienceStorage implements INBTSerializable<NBTTagCompound> {
        // the total level
        public int experienceLevel;
        // the total amount of experience, which is the experience for each level and the experience in the bar
        public int experienceTotal;
        // the experience in the experience bar
        public float experience;

        public void addExperienceLevel(int levels) {
            this.experienceLevel += levels;
            if (this.experienceLevel < 0) {
                this.experienceLevel = 0;
                this.experience = 0.0F;
                this.experienceTotal = 0;
            }
            // edit: don't play the sound
        }

        public int xpBarCap() {
            if (this.experienceLevel >= 30) {
                return 112 + (this.experienceLevel - 30) * 9;
            } else {
                return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
            }
        }

        // edit: add a float instead of an int
        public void addExperience(float amount) {
            int i = Integer.MAX_VALUE - this.experienceTotal;
            if (amount > i)
                amount = i;
            this.experience += amount / this.xpBarCap();
            for (this.experienceTotal += amount; this.experience >= 1.0F; this.experience /= this.xpBarCap()) {
                this.experience = (this.experience - 1.0F) * this.xpBarCap();
                this.addExperienceLevel(1);
            }
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("level", this.experienceLevel);
            tag.setInteger("total", this.experienceTotal);
            tag.setFloat("experience", this.experience);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            this.experienceLevel = nbt.getInteger("level");
            this.experienceTotal = nbt.getInteger("total");
            this.experience = nbt.getFloat("experience");
        }
    }
}
