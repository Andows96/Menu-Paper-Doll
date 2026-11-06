package dev.andows96.menupaperdoll.client;

/**
 * Left-click drag state for the paper doll.
 *
 * <p>While a drag is in progress the doll stops looking at the cursor and becomes a rigid object that
 * the player spins horizontally. Releasing the button keeps the angle the doll was turned to; the first
 * cursor movement afterwards hands control back to the normal look-at-mouse behaviour, with the drag
 * offset easing back to zero so the doll does not jump.</p>
 *
 * <p>Press and release come from the raw mouse button event (the one fired before any screen sees the
 * click); the per-frame {@link #update} call only accumulates the drag delta, so the drag does not
 * depend on how the title screen dispatches input.</p>
 *
 * <p>All methods run on the render thread.</p>
 */
public final class DollDrag {
    /**
     * Degrees of body rotation per <em>window</em> pixel dragged. Raw window pixels instead of GUI
     * pixels keep the feel identical at every GUI scale, and they are also the finest cursor resolution
     * available: with GUI pixels a slow drag would advance in whole GUI pixels, which at large GUI
     * scales shows up as the doll ticking forward instead of turning smoothly.
     */
    private static final float DEGREES_PER_PIXEL = 1.2F;
    /** Fraction of the remaining drag offset that is eased out per frame once the cursor follows again. */
    private static final float FOLLOW_DECAY = 0.15F;
    /**
     * Fraction of the way the drawn angle moves towards the accumulated one per frame. Blends the
     * whole-pixel cursor steps into a continuous rotation; sub-frame latency, invisible in practice.
     */
    private static final float SMOOTHING = 0.55F;
    /** Angles below this many degrees snap shut. */
    private static final float MIN_ANGLE = 0.05F;

    private static boolean dragging;
    private static boolean holding;
    /** Angle accumulated from the drag, in degrees. */
    private static float targetAngle;
    /** Angle actually drawn, smoothed towards {@link #targetAngle}. */
    private static float displayAngle;
    /** Last cursor position seen while dragging, in window pixels. */
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY;
    /** Cursor position at the moment the button was released. */
    private static double releaseMouseX;
    private static double releaseMouseY;

    private DollDrag() {}

    /** Starts a drag. Called when the left button goes down over the doll. */
    public static void begin() {
        dragging = true;
        holding = false;
        lastMouseX = Double.NaN;
    }

    /** Ends the drag and holds the resulting angle. Called on button release. */
    public static void end() {
        if (!dragging) {
            return;
        }
        dragging = false;
        holding = true;
        targetAngle = wrap(targetAngle);
        releaseMouseX = lastMouseX;
        releaseMouseY = lastMouseY;
    }

    /**
     * Advances the drag state for this frame, accumulating the cursor delta while dragging.
     *
     * @param mouseX      cursor X in window pixels (not GUI pixels - see {@link #DEGREES_PER_PIXEL})
     * @param mouseY      cursor Y in window pixels
     * @param sensitivity multiplier on {@link #DEGREES_PER_PIXEL}; 0 turns the drag into a no-op
     */
    public static void update(double mouseX, double mouseY, float sensitivity) {
        if (dragging) {
            if (Double.isNaN(lastMouseX)) {
                // First frame after the press: remember where the drag started.
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            } else {
                targetAngle -= (float) (mouseX - lastMouseX) * DEGREES_PER_PIXEL * sensitivity;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        } else if (holding) {
            // Keeping the angle only lasts until the cursor is moved again.
            if (mouseX != releaseMouseX || mouseY != releaseMouseY) {
                holding = false;
            }
        } else if (targetAngle != 0.0F) {
            // Handing control back to the cursor: ease the drag offset out along the shortest path
            // instead of snapping.
            targetAngle -= wrap(targetAngle) * FOLLOW_DECAY;
            if (Math.abs(wrap(targetAngle)) < MIN_ANGLE) {
                targetAngle = 0.0F;
            }
        }

        if (displayAngle != targetAngle) {
            float remaining = wrap(targetAngle - displayAngle);
            if (Math.abs(remaining) < MIN_ANGLE) {
                displayAngle = targetAngle;
            } else {
                displayAngle += remaining * SMOOTHING;
            }
        }
    }

    /** {@return whether the left button is currently dragging the doll} */
    public static boolean isDragging() {
        return dragging;
    }

    /** {@return whether the doll is a rigid object right now (dragging, or holding a dragged angle)} */
    public static boolean isRigid() {
        return dragging || holding;
    }

    /** {@return the extra body yaw in degrees} */
    public static float angleDeg() {
        return displayAngle;
    }

    /** Normalises to {@code (-180, 180]} so angles always take the shortest way round. */
    private static float wrap(float degrees) {
        float wrapped = (degrees + 180.0F) % 360.0F;
        if (wrapped < 0.0F) {
            wrapped += 360.0F;
        }
        return wrapped - 180.0F;
    }
}
