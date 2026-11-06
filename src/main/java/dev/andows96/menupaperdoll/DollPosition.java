package dev.andows96.menupaperdoll;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * Anchor presets for the doll. Each entry stores the point the doll is centred on, as a fraction of
 * the screen width / height.
 */
public enum DollPosition implements TranslatableEnum {
    /** Left side, slightly below the vertical centre (the intended default). */
    DEFAULT(0.14F, 0.55F),
    /** Right side, slightly below the vertical centre. */
    RIGHT(0.86F, 0.55F),
    TOP_LEFT(0.14F, 0.25F),
    TOP_RIGHT(0.86F, 0.25F),
    BOTTOM_LEFT(0.14F, 0.80F),
    BOTTOM_RIGHT(0.86F, 0.80F);

    private final float centerXRatio;
    private final float centerYRatio;

    DollPosition(float centerXRatio, float centerYRatio) {
        this.centerXRatio = centerXRatio;
        this.centerYRatio = centerYRatio;
    }

    public float centerXRatio() {
        return this.centerXRatio;
    }

    public float centerYRatio() {
        return this.centerYRatio;
    }

    @Override
    public Component getTranslatedName() {
        return Component.translatable("menu_paper_doll.configuration.position." + this.name().toLowerCase(Locale.ROOT));
    }
}
