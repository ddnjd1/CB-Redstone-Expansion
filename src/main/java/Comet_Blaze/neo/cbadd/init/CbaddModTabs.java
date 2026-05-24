package Comet_Blaze.neo.cbadd.init;

import Comet_Blaze.neo.cbadd.CbaddMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CbaddModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CbaddMod.MODID);

    public static final Supplier<CreativeModeTab> TAB_PIKVA = CREATIVE_MODE_TABS.register("tabpikva",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.cbadd.tabpikva"))
                    .icon(() -> new ItemStack(CbaddModItems.CONNECTOR.get().asItem())) // 使用 get() 获取实际方块
                    .displayItems((parameters, output) -> {
                        // 无线红石设备
                        output.accept(CbaddModItems.CONNECTOR.get());
                        output.accept(CbaddModItems.INPUT_BLOCK.get());
                        output.accept(CbaddModItems.OUTPUT_BLOCK.get());
                        output.accept(CbaddModItems.KEY_TRIGGER_BLOCK_ITEM.get());
                        output.accept(CbaddModItems.WIGHTLESS_CONNECTOR_ITEM.get());

                    })
                    .build());
}
