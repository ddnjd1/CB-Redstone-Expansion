package Comet_Blaze.neo.cbadd.network;

import Comet_Blaze.neo.cbadd.CbaddMod;
import Comet_Blaze.neo.cbadd.item.KeyTriggerBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BindKeyPacket(InteractionHand hand, BlockPos pos, String key) implements CustomPacketPayload {

    public static final Type<BindKeyPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CbaddMod.MODID, "bind_key"));

    public static final StreamCodec<FriendlyByteBuf, BindKeyPacket> STREAM_CODEC = StreamCodec.composite(
            new StreamCodec<>() {
                public InteractionHand decode(FriendlyByteBuf buf) { return buf.readEnum(InteractionHand.class); }
                public void encode(FriendlyByteBuf buf, InteractionHand val) { buf.writeEnum(val); }
            },
            BindKeyPacket::hand,
            BlockPos.STREAM_CODEC,
            BindKeyPacket::pos,
            ByteBufCodecs.STRING_UTF8,
            BindKeyPacket::key,
            BindKeyPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handleServer(final BindKeyPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            ItemStack stack = context.player().getItemInHand(packet.hand());
            switch (packet.key()) {
                case "REMOVE" -> KeyTriggerBlockItem.removeBind(stack, packet.pos());
                case "CLEAR_ALL" -> KeyTriggerBlockItem.removeAllBindsForPos(stack, packet.pos());
                default -> KeyTriggerBlockItem.addBind(stack, packet.pos(), packet.key());
            }
        });
    }
}
