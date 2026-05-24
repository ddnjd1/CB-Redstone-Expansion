package Comet_Blaze.neo.cbadd.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RedstoneConnectorConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_OUTPUTS_PER_INPUT;

    static {
        BUILDER.push("Redstone Connector Settings");
        MAX_OUTPUTS_PER_INPUT = BUILDER
                .comment("设置无线红石最多输出端(默认32) Maximum number of output blocks that can be connected to a input block(default: 32)")
                .defineInRange("maxOutputsPerInput", 32, 1, 256);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
