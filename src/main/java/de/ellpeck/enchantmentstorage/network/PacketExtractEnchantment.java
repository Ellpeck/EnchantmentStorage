package de.ellpeck.enchantmentstorage.network;

import de.ellpeck.enchantmentstorage.TileEnchantmentStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketExtractEnchantment implements IMessage {

    private BlockPos pos;
    private ResourceLocation enchantment;
    private int level;

    public PacketExtractEnchantment(BlockPos pos, ResourceLocation enchantment, int level) {
        this.pos = pos;
        this.enchantment = enchantment;
        this.level = level;
    }

    public PacketExtractEnchantment() {

    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packet = new PacketBuffer(buf);
        this.pos = packet.readBlockPos();
        this.enchantment = new ResourceLocation(packet.readString(256));
        this.level = packet.readVarInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packet = new PacketBuffer(buf);
        packet.writeBlockPos(this.pos);
        packet.writeString(this.enchantment.toString());
        packet.writeVarInt(this.level);
    }

    public static class Handler implements IMessageHandler<PacketExtractEnchantment, IMessage> {

        @Override
        public IMessage onMessage(PacketExtractEnchantment message, MessageContext ctx) {
            WorldServer world = ctx.getServerHandler().player.getServerWorld();
            world.addScheduledTask(() -> {
                TileEntity tile = world.getTileEntity(message.pos);
                if (tile instanceof TileEnchantmentStorage)
                    ((TileEnchantmentStorage) tile).createEnchantedBook(message.enchantment, message.level);
            });
            return null;
        }
    }
}