package dev.andows96.menupaperdoll;

import dev.andows96.menupaperdoll.compat.DragonWingsCompat;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config for Menu Paper Doll, editable both as {@code config/menu_paper_doll-client.toml} and
 * through the standard NeoForge config screen (Mods list -> Menu Paper Doll -> Config).
 */
public final class MenuPaperDollConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<YesNo> ENABLED = BUILDER
            .translation("menu_paper_doll.configuration.enabled")
            .defineEnum("enabled", YesNo.YES);

    public static final ModConfigSpec.EnumValue<DollPosition> POSITION = BUILDER
            .translation("menu_paper_doll.configuration.position")
            .defineEnum("position", DollPosition.RIGHT);

    public static final ModConfigSpec.IntValue OFFSET_X = BUILDER
            .comment("Moves the doll horizontally, in percent of the screen width.",
                    "Negative values move it left, positive values move it right.",
                    "The doll is kept inside the screen, so it stops at the edges.")
            .translation("menu_paper_doll.configuration.offsetX")
            .defineInRange("offsetX", 0, -50, 50);

    public static final ModConfigSpec.IntValue OFFSET_Y = BUILDER
            .comment("Moves the doll vertically, in percent of the screen height.",
                    "Negative values move it up, positive values move it down.",
                    "The doll is kept inside the screen, so it stops at the edges.")
            .translation("menu_paper_doll.configuration.offsetY")
            .defineInRange("offsetY", 0, -50, 50);

    public static final ModConfigSpec.IntValue SCALE_PERCENT = BUILDER
            .comment("Doll size in percent of the screen height, as shown on the title screen.",
                    "100 is the default size (one third of the screen height).",
                    "Lower values make the doll smaller.")
            .translation("menu_paper_doll.configuration.scalePercent")
            .defineInRange("scalePercent", 100, 30, 100);

    public static final ModConfigSpec.EnumValue<FollowMode> FOLLOW_MODE = BUILDER
            .comment("How the doll follows the mouse cursor.",
                    "BODY_AND_HEAD: the whole body turns towards the cursor, the head turns with it.",
                    "HEAD_ONLY: the body keeps facing you and only the head turns towards the cursor.")
            .translation("menu_paper_doll.configuration.followMode")
            .defineEnum("followMode", FollowMode.BODY_AND_HEAD);

    public static final ModConfigSpec.IntValue ROTATION_SENSITIVITY = BUILDER
            .translation("menu_paper_doll.configuration.rotationSensitivity")
            .defineInRange("rotationSensitivity", 50, 0, 100);

    /**
     * Only registered when Dragon Wings: Rebuild is installed - the option is meaningless otherwise and
     * would just be dead weight in the config screen.
     */
    public static final ModConfigSpec.BooleanValue SHOW_DRAGON_WINGS = DragonWingsCompat.isAvailable()
            ? BUILDER
                .comment("Show the dragon wings behind the paper doll.",
                        "The wings come from Dragon Wings: Rebuild and follow that mod's own settings:",
                        "the first wing type enabled there is used, at its configured scale.")
                .translation("menu_paper_doll.configuration.showDragonWings")
                .define("showDragonWings", true)
            : null;

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MenuPaperDollConfig() {}

    /** {@return whether the doll is switched on in the config} */
    public static boolean isEnabled() {
        return SPEC.isLoaded() && ENABLED.get() == YesNo.YES;
    }

    /** {@return whether the doll should render dragon wings this frame} */
    public static boolean shouldShowDragonWings() {
        return SHOW_DRAGON_WINGS != null && isEnabled() && SHOW_DRAGON_WINGS.get();
    }

    /**
     * {@return the drag sensitivity as a multiplier of the default tuning}, where 1 means the shipped
     * 1.2 degrees per pixel and 0 disables dragging entirely
     */
    public static float rotationSensitivityFactor() {
        return ROTATION_SENSITIVITY.get() / 50.0F;
    }
}
