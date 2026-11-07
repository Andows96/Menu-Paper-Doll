package dev.andows96.menupaperdoll;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * A plain yes/no switch. Used instead of a boolean value so the config screen can show 是/否
 * (NeoForge renders booleans with the vanilla on/off labels, which cannot be changed by a mod).
 */
public enum YesNo implements TranslatableEnum {
    YES,
    NO;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("menu_paper_doll.configuration.enabled." + this.name().toLowerCase(Locale.ROOT));
    }
}
