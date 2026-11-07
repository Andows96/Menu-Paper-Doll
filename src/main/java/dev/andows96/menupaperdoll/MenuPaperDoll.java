package dev.andows96.menupaperdoll;

import com.mojang.blaze3d.platform.InputConstants;
import dev.andows96.menupaperdoll.client.DollConfigScreen;
import dev.andows96.menupaperdoll.client.DollDrag;
import dev.andows96.menupaperdoll.client.DollRenderer;
import dev.andows96.menupaperdoll.compat.DragonWingsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Menu Paper Doll - draws a small paper doll on the title screen that turns to look at the mouse
 * cursor, and can be spun around with a left-click drag.
 *
 * <p>This is a client-only mod: {@code dist = Dist.CLIENT} keeps the class from ever loading on a
 * dedicated server.</p>
 */
@Mod(value = MenuPaperDoll.MODID, dist = Dist.CLIENT)
public final class MenuPaperDoll {
    public static final String MODID = "menu_paper_doll";
    /** {@code GLFW_MOUSE_BUTTON_LEFT}, spelled out so the LWJGL classes stay off the class path. */
    private static final int LEFT_BUTTON = 0;

    public MenuPaperDoll(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, MenuPaperDollConfig.SPEC);
        // Standard NeoForge config screen (with the description-less options trimmed), so Configured /
        // Catalogue can pick it up as well.
        container.registerExtensionPoint(IConfigScreenFactory.class, DollConfigScreen::create);

        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Foreground event) -> onScreenRender(event));
        NeoForge.EVENT_BUS.addListener((InputEvent.MouseButton.Pre event) -> onRawMouseButton(event));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> onMousePressed(event));
        NeoForge.EVENT_BUS.addListener(MenuPaperDoll::onPlayerRendered);
    }

    /**
     * Fired for every screen right after its own contents have been recorded. We only draw on the
     * title screen, and the doll is submitted through the vanilla picture-in-picture entity pipeline.
     */
    private static void onScreenRender(ScreenEvent.Render.Foreground event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            DollRenderer.render(event.getGuiGraphics(), screen.width, screen.height, event.getMouseX(), event.getMouseY());
        }
    }

    /**
     * Raw mouse button hook: fired before any screen sees the click, so it cannot be swallowed by a
     * widget. Pressing over the doll starts a drag; releasing ends it.
     *
     * <p>The event carries no coordinates, so the hit test uses the cursor position remembered from
     * the last rendered frame - which is at most one frame old.</p>
     */
    private static void onRawMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != InputConstants.MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        if (!(screen instanceof TitleScreen title)) {
            return;
        }
        if (event.getAction() == InputConstants.PRESS) {
            // Drag sensitivity 0 means the feature is off: keep the normal look-at-mouse behaviour
            // instead of freezing the doll for the duration of the click.
            if (MenuPaperDollConfig.rotationSensitivityFactor() > 0.0F
                && DollRenderer.isOverDoll(title.width, title.height, DollRenderer.lastMouseX(), DollRenderer.lastMouseY())) {
                DollDrag.begin();
            }
        } else if (event.getAction() == InputConstants.RELEASE) {
            DollDrag.end();
        }
    }

    /**
     * A left click on the doll starts a drag instead of reaching the screen. Clicking anywhere else is
     * left alone, so the regular buttons keep working.
     */
    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != LEFT_BUTTON) {
            return;
        }
        if (!(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        if (DollRenderer.isOverDoll(screen.width, screen.height, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    /**
     * Add-on hook: the avatar renderer posts this event for the paper doll as well, because the doll is
     * drawn through the real player renderer. Only react to the doll's own render state.
     */
    private static void onPlayerRendered(RenderPlayerEvent.Post<?> event) {
        AvatarRenderState state = event.getRenderState();
        if (state != DollRenderer.lastSubmittedState() || !MenuPaperDollConfig.shouldShowDragonWings()) {
            return;
        }
        DragonWingsCompat.renderWings(state, event.getPoseStack(), event.getSubmitNodeCollector());
    }
}
