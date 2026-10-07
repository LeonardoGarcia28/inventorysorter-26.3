package dev.leonardo.totemaccessorycompat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TotemAccessoryClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue AMULET_X;
    public static final ModConfigSpec.DoubleValue AMULET_Y;
    public static final ModConfigSpec.DoubleValue AMULET_Z;
    public static final ModConfigSpec.DoubleValue AMULET_SCALE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.translation("totem_accessory_263.configuration.visuals")
                .comment("Visual placement of the equipped Totem amulet.")
                .push("visuals");

        AMULET_X = builder
                .translation("totem_accessory_263.configuration.amulet_x")
                .comment("Horizontal offset. Negative = left, positive = right.")
                .defineInRange("amuletX", 0.0D, -2.0D, 2.0D);

        AMULET_Y = builder
                .translation("totem_accessory_263.configuration.amulet_y")
                .comment("Vertical offset around the chest anchor. Positive = up.")
                .defineInRange("amuletY", 0.13D, -2.0D, 2.0D);

        AMULET_Z = builder
                .translation("totem_accessory_263.configuration.amulet_z")
                .comment("Depth offset. More negative moves the Totem forward from the chest.")
                .defineInRange("amuletZ", -0.155D, -2.0D, 2.0D);

        AMULET_SCALE = builder
                .translation("totem_accessory_263.configuration.amulet_scale")
                .comment("Visual size of the Totem amulet.")
                .defineInRange("amuletScale", 0.20D, 0.05D, 1.00D);

        builder.pop();

        SPEC = builder.build();
    }

    private TotemAccessoryClientConfig() {}
}
