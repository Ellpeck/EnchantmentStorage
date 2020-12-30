package de.ellpeck.enchantmentstorage.network;

import de.ellpeck.enchantmentstorage.TileEnchantmentStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketExtractExperience implements IMessage {

    private BlockPos pos;
    private int amount;

    public PacketExtractExperience(BlockPos pos, int amount) {
        this.pos = pos;
        this.amount = amount;
    }

    public PacketExtractExperience() {

    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packet = new PacketBuffer(buf);
        this.pos = packet.readBlockPos();
        this.amount = packet.readVarInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packet = new PacketBuffer(buf);
        packet.writeBlockPos(this.pos);
        packet.writeVarInt(this.amount);
    }

    public static class Handler implements IMessageHandler<PacketExtractExperience, IMessage> {

        @Override
        public IMessage onMessage(PacketExtractExperience message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity tile = player.world.getTileEntity(message.pos);
                if (tile instanceof TileEnchantmentStorage)
                    ((TileEnchantmentStorage) tile).extractFromPlayer(player, message.amount);
            });
            return null;
        }
    }
}