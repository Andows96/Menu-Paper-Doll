package dev.andows96.menupaperdoll;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/** How the doll reacts to the mouse cursor. */
public enum FollowMode implements TranslatableEnum {
    /** The whole body turns towards the cursor and the head turns along with it. */
    BODY_AND_HEAD,
    /** The body keeps facing the viewer and only the head turns towards the cursor. */
    HEAD_ONLY;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("menu_paper_doll.configuration.followMode." + this.name().toLowerCase(Locale.ROOT));
    }
}
