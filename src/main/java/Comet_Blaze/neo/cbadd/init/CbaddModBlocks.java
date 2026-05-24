package Comet_Blaze.neo.cbadd.init;

import Comet_Blaze.neo.cbadd.CbaddMod;
import Comet_Blaze.neo.cbadd.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CbaddModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CbaddMod.MODID);
    public static final DeferredBlock<ConnectorBlock> INPUT_BLOCK = BLOCKS.register("input_block", () -> new ConnectorBlock(true));
    public static final DeferredBlock<ConnectorBlock> OUTPUT_BLOCK = BLOCKS.register("output_block", () -> new ConnectorBlock(false));
    public static final DeferredBlock<KeyTriggerBlock> KEY_TRIGGER_BLOCK =
            BLOCKS.register("key_trigger_block", KeyTriggerBlock::new);
    public static final DeferredBlock<WightlessConnectorBlock> WIGHTLESS_CONNECTOR =
            BLOCKS.register("wightless_connector", WightlessConnectorBlock::new);
}
