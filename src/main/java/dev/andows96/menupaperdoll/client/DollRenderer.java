package dev.andows96.menupaperdoll.client;

import com.mojang.authlib.GameProfile;
import dev.andows96.menupaperdoll.DollPosition;
import dev.andows96.menupaperdoll.FollowMode;
import dev.andows96.menupaperdoll.MenuPaperDollConfig;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders the title screen paper doll.
 *
 * <p>The doll is submitted through vanilla's picture-in-picture entity pipeline
 * ({@link GuiGraphicsExtractor#entity}), the same way {@code InventoryScreen} draws the player model
 * next to the inventory, so the pose, skin layers and lighting all match vanilla.</p>
 */
public final class DollRenderer {
    /** Player model height in blocks; one block equals one render unit here. */
    private static final float MODEL_HEIGHT = 1.875F;
    /** Player hitbox, used to place the model inside the render rectangle exactly like vanilla does. */
    private static final float PLAYER_WIDTH = 0.6F;
    private static final float PLAYER_HEIGHT = 1.8F;
    /** Default doll height, as a fraction of the screen height. */
    private static final float DEFAULT_HEIGHT_RATIO = 1.0F / 3.0F;
    /** Config value that maps to {@link #DEFAULT_HEIGHT_RATIO}; 90 in the config is the default size. */
    private static final float BASELINE_SCALE_PERCENT = 90.0F;
    /** Render rectangle size relative to the doll height (extra margin so the model never gets clipped). */
    private static final float BOX_RATIO = 1.4F;
    /** How long we wait for the player skin before falling back to the default Steve/Alex skin. */
    private static final long SKIN_WAIT_MS = 1000L;

    private static CompletableFuture<Optional<PlayerSkin>> skinFuture;
    private static GameProfile skinProfile;
    private static long skinRequestStart;
    /**
     * The render state submitted for the current frame. Add-on integrations compare against it to tell
     * the paper doll's render pass apart from a real player's.
     */
    private static AvatarRenderState lastSubmittedState;
    /** Cursor position of the last rendered frame, used by the mouse-button hook to hit test. */
    private static double lastMouseX;
    private static double lastMouseY;

    private DollRenderer() {}

    /** {@return the render state submitted this frame, or {@code null} if the doll was not drawn} */
    public static AvatarRenderState lastSubmittedState() {
        return lastSubmittedState;
    }

    /** {@return the cursor X of the last rendered frame} */
    public static double lastMouseX() {
        return lastMouseX;
    }

    /** {@return the cursor Y of the last rendered frame} */
    public static double lastMouseY() {
        return lastMouseY;
    }

    /**
     * Draws the doll for this frame, or draws nothing when the mod is disabled, the window is too
     * small, or the skin is still downloading.
     *
     * @param graphics     the GUI graphics of the screen being rendered
     * @param screenWidth  screen width in GUI pixels
     * @param screenHeight screen height in GUI pixels
     * @param mouseX       mouse X in GUI pixels
     * @param mouseY       mouse Y in GUI pixels
     */
    public static void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        lastSubmittedState = null;

        if (!MenuPaperDollConfig.isEnabled()) {
            return;
        }
        DollLayout layout = layout(screenWidth, screenHeight);
        if (layout == null) {
            // Window too small for the requested doll size.
            return;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
        // Raw window pixels: the GUI-scaled cursor only changes every `guiScale` pixels, which makes a
        // slow drag advance in visible steps.
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        DollDrag.update(mouse.xpos(), mouse.ypos(), MenuPaperDollConfig.rotationSensitivityFactor());

        PlayerSkin skin = resolveSkin();
        if (skin == null) {
            // Skin is still downloading - draw nothing this frame instead of flashing a Steve/Alex doll.
            return;
        }

        FollowMode followMode = MenuPaperDollConfig.FOLLOW_MODE.get();
        // In the picture-in-picture pipeline one render unit is `size` GUI pixels, so this keeps the
        // doll's on-screen height at `dollHeight` pixels regardless of the GUI scale setting.
        float size = layout.dollHeight() / MODEL_HEIGHT;

        // Vanilla inventory maths (see InventoryScreen#extractEntityInInventoryFollowsMouse), with the
        // head either following the body (BODY_AND_HEAD) or keeping the body still (HEAD_ONLY).
        float xAngle = (float) Math.atan((layout.centerX() - mouseX) / 40.0F);
        float yAngle = (float) Math.atan((layout.centerY() - mouseY) / 40.0F);

        // Horizontal turn and vertical pitch in degrees, the same quantities vanilla feeds into the
        // body rotation and the head.
        float turn = xAngle * 20.0F;
        float pitch = -yAngle * 20.0F;
        // Extra yaw from left-click dragging; it survives the release until the cursor is moved again.
        float dragAngle = DollDrag.angleDeg();
        boolean rigid = DollDrag.isRigid();

        // Vanilla tilts the whole model towards the cursor and cancels that tilt again with the
        // camera angle, so the body stays upright and only the head visibly looks up and down.
        // BODY_AND_HEAD keeps that arrangement. In HEAD_ONLY the tilt is applied to neither the
        // model nor the camera: the head carries the full pitch by itself, and skipping the
        // camera tilt keeps the whole doll from sliding up and down with the cursor.
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraAngle = !rigid && followMode == FollowMode.BODY_AND_HEAD
            ? new Quaternionf().rotateX(-pitch * (float) (Math.PI / 180.0))
            : new Quaternionf();
        rotation.mul(cameraAngle);

        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        if (rigid) {
            // While dragging (and while the dragged angle is held) the doll is a rigid object: the
            // body carries the drag, and neither the head nor the camera follows the cursor.
            state.bodyRot = 180.0F + dragAngle;
            state.yRot = 0.0F;
            state.xRot = 0.0F;
        } else if (followMode == FollowMode.BODY_AND_HEAD) {
            state.bodyRot = 180.0F + turn + dragAngle;
            // A relative head yaw of zero keeps the head aligned with the body.
            state.yRot = 0.0F;
            state.xRot = pitch;
        } else {
            // Body keeps facing the viewer; the head alone carries the whole turn.
            state.bodyRot = 180.0F + dragAngle;
            state.yRot = turn;
            state.xRot = pitch;
        }
        state.boundingBoxWidth = PLAYER_WIDTH;
        state.boundingBoxHeight = PLAYER_HEIGHT;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.isInvisible = false;
        state.isSpectator = false;

        Vector3f translation = new Vector3f(0.0F, PLAYER_HEIGHT / 2.0F + 0.0625F, 0.0F);
        lastSubmittedState = state;
        graphics.entity(state, size, translation, rotation, cameraAngle, layout.x0(), layout.y0(), layout.x1(), layout.y1());
    }

    /**
     * Hit test used to decide whether a left click grabs the doll instead of a button.
     *
     * @return {@code true} if the cursor is over the doll's silhouette on the title screen
     */
    public static boolean isOverDoll(int screenWidth, int screenHeight, double mouseX, double mouseY) {
        if (!MenuPaperDollConfig.isEnabled()) {
            return false;
        }
        DollLayout layout = layout(screenWidth, screenHeight);
        return layout != null && layout.contains(mouseX, mouseY);
    }

    /**
     * Computes the picture-in-picture rectangle for the configured position and size, clamped to stay
     * on screen.
     *
     * @return the layout, or {@code null} if the window is too small to draw the doll
     */
    private static DollLayout layout(int screenWidth, int screenHeight) {
        DollPosition position = MenuPaperDollConfig.POSITION.get();
        float scalePercent = MenuPaperDollConfig.SCALE_PERCENT.get().floatValue();

        float dollHeight = screenHeight * DEFAULT_HEIGHT_RATIO * (scalePercent / BASELINE_SCALE_PERCENT);
        float halfBox = dollHeight * BOX_RATIO * 0.5F;
        // Manual fine tuning on top of the preset anchor, in percent of the screen size.
        float anchorX = screenWidth * position.centerXRatio()
            + screenWidth * (MenuPaperDollConfig.OFFSET_X.get() / 100.0F);
        float anchorY = screenHeight * position.centerYRatio()
            + screenHeight * (MenuPaperDollConfig.OFFSET_Y.get() / 100.0F);

        int x0 = Math.round(anchorX - halfBox);
        int x1 = Math.round(anchorX + halfBox);
        int y0 = Math.round(anchorY - halfBox);
        int y1 = Math.round(anchorY + halfBox);

        // Keep the render rectangle fully on screen; the doll is centred inside it.
        if (x0 < 0) {
            x1 -= x0;
            x0 = 0;
        }
        if (x1 > screenWidth) {
            x0 -= x1 - screenWidth;
            x1 = screenWidth;
        }
        if (y0 < 0) {
            y1 -= y0;
            y0 = 0;
        }
        if (y1 > screenHeight) {
            y0 -= y1 - screenHeight;
            y1 = screenHeight;
        }
        x0 = Math.max(x0, 0);
        y0 = Math.max(y0, 0);

        if (x1 - x0 < 4 || y1 - y0 < 4) {
            return null;
        }
        return new DollLayout(x0, y0, x1, y1, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, dollHeight);
    }

    /**
     * Resolves the local player's skin, waiting at most {@link #SKIN_WAIT_MS} for the texture to
     * download. Returns {@code null} while the lookup is still pending so the caller can skip drawing.
     */
    private static PlayerSkin resolveSkin() {
        Minecraft minecraft = Minecraft.getInstance();
        GameProfile profile = minecraft.getGameProfile();
        if (profile == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }

        long now = System.currentTimeMillis();
        if (skinFuture == null || !profile.equals(skinProfile)) {
            skinProfile = profile;
            skinFuture = minecraft.getSkinManager().get(profile);
            skinRequestStart = now;
        }

        try {
            Optional<PlayerSkin> loaded = skinFuture.getNow(null);
            if (loaded != null) {
                return loaded.orElseGet(() -> DefaultPlayerSkin.get(profile));
            }
        } catch (Exception ignored) {
            return DefaultPlayerSkin.get(profile);
        }

        return now - skinRequestStart >= SKIN_WAIT_MS ? DefaultPlayerSkin.get(profile) : null;
    }

    /**
     * Where the doll is drawn. The render rectangle carries a margin around the model, so the hit test
     * uses a tighter box around the silhouette instead of the whole rectangle.
     */
    private record DollLayout(int x0, int y0, int x1, int y1, float centerX, float centerY, float dollHeight) {
        boolean contains(double mouseX, double mouseY) {
            float halfWidth = dollHeight * 0.30F;
            float halfHeight = dollHeight * 0.5F;
            return mouseX >= centerX - halfWidth
                && mouseX <= centerX + halfWidth
                && mouseY >= centerY - halfHeight
                && mouseY <= centerY + halfHeight;
        }
    }
}
