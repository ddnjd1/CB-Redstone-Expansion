package Comet_Blaze.neo.cbadd.network;

import Comet_Blaze.neo.cbadd.CbaddMod;
import Comet_Blaze.neo.cbadd.entity.KeyTriggerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TriggerKeyPacket(BlockPos blockPos, String key, boolean pressed) implements CustomPacketPayload {

    public static final Type<TriggerKeyPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CbaddMod.MODID, "trigger_key"));

    public static final StreamCodec<FriendlyByteBuf, TriggerKeyPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TriggerKeyPacket::blockPos,
            ByteBufCodecs.STRING_UTF8,
            TriggerKeyPacket::key,
            ByteBufCodecs.BOOL,
            TriggerKeyPacket::pressed,
            TriggerKeyPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleServer(final TriggerKeyPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            ServerLevel level = (ServerLevel) context.player().level();
            if (level.getBlockEntity(packet.blockPos()) instanceof KeyTriggerBlockEntity entity) {
                if (packet.pressed()) {
                    entity.onKeyPressed(packet.key());
                } else {
                    entity.onKeyReleased(packet.key());
                }
            }
        });
    }
}
