package de.ellpeck.enchantmentstorage;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketSetExperience;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
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
    public static final int XP_ITEM_IN_SLOT = 2;
    public final ItemStackHandler items = new ItemStackHandler(3) {
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
            if (slot == XP_ITEM_IN_SLOT)
                return Config.xpItems.containsKey(stack.getItem().getRegistryName());
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
            boolean dirty = false;

            // store enchantments
            ItemStack book = this.items.getStackInSlot(BOOK_IN_SLOT);
            if (!book.isEmpty() && book.getItem() == Items.ENCHANTED_BOOK) {
                for (Map.Entry<Enchantment, Integer> ench : EnchantmentHelper.getEnchantments(book).entrySet()) {
                    MutableInt current = this.storedEnchantments.computeIfAbsent(ench.getKey().getRegistryName(), e -> new MutableInt());
                    current.add(getLevelOneCount(ench.getValue()));
                }
                book.shrink(1);
                dirty = true;
            }

            // convert tank contents to stored xp
            if (this.tank.getFluidAmount() > 0) {
                float added = Config.xpFluids.getOrDefault(this.tank.getFluid().getFluid().getName(), 0F);
                if (added > 0) {
                    FluidStack drained = this.tank.drainInternal(100, true);
                    this.experience.addExperience(added * drained.amount);
                    dirty = true;
                }
            }

            // store xp items
            ItemStack xp = this.items.getStackInSlot(XP_ITEM_IN_SLOT);
            if (!xp.isEmpty()) {
                float added = Config.xpItems.getOrDefault(xp.getItem().getRegistryName(), 0F);
                if (added > 0) {
                    xp.shrink(1);
                    this.experience.addExperience(added);
                    dirty = true;
                }
            }

            // twerking
            if (Config.twerkXp > 0) {
                AxisAlignedBB area = new AxisAlignedBB(this.pos).grow(3);
                for (EntityPlayer player : this.world.getEntitiesWithinAABB(EntityPlayer.class, area, EntitySelectors.NOT_SPECTATING)) {
                    NBTTagCompound data = player.getEntityData();
                    // did the player just start sneaking?
                    if (!data.getBoolean(EnchantmentStorage.ID + ":sneaking") && player.isSneaking()) {
                        this.experience.addExperience(Config.twerkXp);
                        dirty = true;
                    }
                    data.setBoolean(EnchantmentStorage.ID + ":sneaking", player.isSneaking());
                }
            }

            if (dirty) {
                this.sendToClients();
                this.markDirty();
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
        // TODO backwards compat, remove before release
        if (this.items.getSlots() < 3)
            this.items.setSize(3);
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

    public void extractFromPlayer(EntityPlayerMP player, int amount) {
        int possibleAmount = Math.min(amount, player.experienceLevel);
        if (possibleAmount > 0) {
            // we can't just add levels, because the amount of xp that each level has changes with the level
            while (possibleAmount > 0) {
                player.experienceLevel--;
                // since we just decreased the level, the cap will now be the amount of xp that the previous level represents
                this.experience.addExperience(player.xpBarCap());
                possibleAmount--;
            }

            player.connection.sendPacket(new SPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
            this.sendToClients();
            this.markDirty();
        }
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
