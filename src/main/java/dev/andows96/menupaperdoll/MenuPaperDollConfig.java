package dev.andows96.menupaperdoll;

import dev.andows96.menupaperdoll.compat.DragonWingsCompat;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config for Menu Paper Doll, editable both as {@code config/menu_paper_doll-client.toml} and
 * through the standard NeoForge config screen (Mods list -> Menu Paper Doll -> Config).
 */
public final class MenuPaperDollConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Show the paper doll on the title screen.",
                    "Turning this off hides the doll completely; every other option is ignored.")
            .translation("menu_paper_doll.configuration.enabled")
            .define("enabled", true);

    public static final ModConfigSpec.EnumValue<DollPosition> POSITION = BUILDER
            .comment("Where the doll stands on the title screen.",
                    "Anchors are relative to the screen: left/right is 14% / 86% of the width,",
                    "top/middle/bottom is 25% / 55% / 80% of the height.")
            .translation("menu_paper_doll.configuration.position")
            .defineEnum("position", DollPosition.DEFAULT);

    public static final ModConfigSpec.IntValue SCALE_PERCENT = BUILDER
            .comment("Doll size in percent of the default size (default = one third of the screen height).",
                    "Lower values make the doll smaller, 100 keeps the default size.")
            .translation("menu_paper_doll.configuration.scalePercent")
            .defineInRange("scalePercent", 100, 20, 100);

    public static final ModConfigSpec.EnumValue<FollowMode> FOLLOW_MODE = BUILDER
            .comment("How the doll follows the mouse cursor.",
                    "BODY_AND_HEAD: the whole body turns towards the cursor, the head turns with it.",
                    "HEAD_ONLY: the body keeps facing you and only the head turns towards the cursor.")
            .translation("menu_paper_doll.configuration.followMode")
            .defineEnum("followMode", FollowMode.BODY_AND_HEAD);

    public static final ModConfigSpec.IntValue ROTATION_SENSITIVITY = BUILDER
            .comment("How far the doll turns per pixel the cursor is dragged while the left button is held.",
                    "50 = the default feel, 100 = twice as fast, 0 = dragging does not turn the doll at all.")
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

    /** {@return whether the doll should render dragon wings this frame} */
    public static boolean shouldShowDragonWings() {
        return SHOW_DRAGON_WINGS != null && SPEC.isLoaded() && ENABLED.get() && SHOW_DRAGON_WINGS.get();
    }

    /**
     * {@return the drag sensitivity as a multiplier of the default tuning}, where 1 means the shipped
     * 1.2 degrees per pixel and 0 disables dragging entirely
     */
    public static float rotationSensitivityFactor() {
        return ROTATION_SENSITIVITY.get() / 50.0F;
    }
}
