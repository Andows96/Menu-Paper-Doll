package dev.andows96.menupaperdoll.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Optional integration with <em>Dragon Wings: Rebuild</em>.
 *
 * <p>Nothing from that mod is compiled against: it is an unpublished sibling mod, so the whole hook is
 * resolved reflectively and simply stays dark when the mod is absent. That keeps this project buildable
 * on its own and avoids pulling GPL-3.0 code into an MIT project.</p>
 *
 * <p>The wings themselves are still rendered by that mod's own renderer, which reads its own config
 * (wing type, scale) - we only hand it the paper doll's render state and pose stack.</p>
 */
public final class DragonWingsCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "dragon_wings_rebuild";

    private static boolean probed;
    private static boolean available;
    private static Class<?> wingTypeClass;
    private static Object[] wingTypes;
    private static Method isWingsEnabled;
    private static Method renderWings;

    private DragonWingsCompat() {}

    /**
     * Resolves the integration once. Safe to call during mod construction (config registration).
     *
     * @return {@code true} if the other mod is loaded and its renderer could be resolved
     */
    public static synchronized boolean isAvailable() {
        if (!probed) {
            probed = true;
            available = probe();
        }
        return available;
    }

    private static boolean probe() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            wingTypeClass = Class.forName("dev.andows96.dragonwings.WingType");
            wingTypes = wingTypeClass.getEnumConstants();
            Class<?> configClass = Class.forName("dev.andows96.dragonwings.Config");
            isWingsEnabled = configClass.getMethod("isWingsEnabled", wingTypeClass);
            Class<?> rendererClass = Class.forName("dev.andows96.dragonwings.WingRenderer");
            renderWings = rendererClass.getMethod(
                "renderWings", AvatarRenderState.class, PoseStack.class, SubmitNodeCollector.class, wingTypeClass);
            return true;
        } catch (Throwable t) {
            // Present but not hookable (different version, module isolation, renamed classes...).
            LOGGER.warn("Menu Paper Doll found {} but could not hook its renderer; the wing option will do nothing.", MOD_ID, t);
            return false;
        }
    }

    /**
     * Draws the wings the other mod would draw for the local player behind the paper doll: the first
     * wing type enabled in its config, at whatever scale that config asks for.
     */
    public static void renderWings(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!isAvailable()) {
            return;
        }
        try {
            Object wingType = firstEnabledWingType();
            if (wingType == null) {
                // Every wing type is switched off in the other mod's config.
                return;
            }
            renderWings.invoke(null, state, poseStack, collector, wingType);
        } catch (Throwable t) {
            LOGGER.warn("Menu Paper Doll failed to draw the dragon wings on the paper doll", t);
            // Do not retry every frame: one broken hook should not fill the log.
            available = false;
        }
    }

    private static Object firstEnabledWingType() throws Exception {
        if (wingTypes == null) {
            return null;
        }
        for (Object wingType : wingTypes) {
            if ((Boolean) isWingsEnabled.invoke(null, wingType)) {
                return wingType;
            }
        }
        return null;
    }
}
