package Comet_Blaze.neo.cbadd.init;

import Comet_Blaze.neo.cbadd.CbaddMod;
import Comet_Blaze.neo.cbadd.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CbaddModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CbaddMod.MODID);
    // 无线红石连接工具
    public static final DeferredItem<Item> CONNECTOR = ITEMS.registerItem("connector", props -> new ConnectorItem(props.stacksTo(1)));
    public static final DeferredItem<BlockItem> KEY_TRIGGER_BLOCK_ITEM =
            ITEMS.register("key_trigger_block",
                    props -> new KeyTriggerBlockItem(CbaddModBlocks.KEY_TRIGGER_BLOCK.get(),
                            new Item.Properties().stacksTo(1)));
    // 输入/输出方块的物品形式
    public static final DeferredItem<BlockItem> INPUT_BLOCK = ITEMS.registerSimpleBlockItem("input_block", CbaddModBlocks.INPUT_BLOCK);
    public static final DeferredItem<BlockItem> OUTPUT_BLOCK = ITEMS.registerSimpleBlockItem("output_block", CbaddModBlocks.OUTPUT_BLOCK);
    public static final DeferredItem<BlockItem> WIGHTLESS_CONNECTOR_ITEM =
            ITEMS.registerSimpleBlockItem("wightless_connector", CbaddModBlocks.WIGHTLESS_CONNECTOR);
}
