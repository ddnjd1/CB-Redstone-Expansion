package Comet_Blaze.neo.cbadd.init;

import Comet_Blaze.neo.cbadd.CbaddMod;
import Comet_Blaze.neo.cbadd.entity.ConnectorTileEntity;
import Comet_Blaze.neo.cbadd.entity.KeyTriggerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CbaddModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CbaddMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConnectorTileEntity>> CONNECTOR =
            REGISTRY.register("connector",
                    () -> BlockEntityType.Builder.of(
                            ConnectorTileEntity::new,
                            CbaddModBlocks.INPUT_BLOCK.get(),
                            CbaddModBlocks.OUTPUT_BLOCK.get(),
                            CbaddModBlocks.WIGHTLESS_CONNECTOR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KeyTriggerBlockEntity>> KEY_TRIGGER =
            REGISTRY.register("key_trigger",
                    () -> BlockEntityType.Builder.of(
                            KeyTriggerBlockEntity::new,
                            CbaddModBlocks.KEY_TRIGGER_BLOCK.get()
                    ).build(null));

}