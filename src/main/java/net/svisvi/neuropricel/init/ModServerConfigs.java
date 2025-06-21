package net.svisvi.neuropricel.init;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModServerConfigs {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> VOICE_SERVER_IP;
    public static final ForgeConfigSpec.ConfigValue<String> TEXT_SERVER_IP;
    public static final ForgeConfigSpec.ConfigValue<String> PRICEL_SPEAKER;

    static {
        BUILDER.push("Configs for NeuroPricel mod");

        VOICE_SERVER_IP = BUILDER.comment("AI voice gen server IP")
                .define("Voice server IP", "localhost:14880");
        TEXT_SERVER_IP = BUILDER.comment("AI (or not) text gen server IP")
                .define("Text server IP", "localhost:14880");
        PRICEL_SPEAKER = BUILDER.comment("Speaker for block of pricel")
                .define("Pricel speaker", "pricelius_v2");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
