package com.starboundmc.client;

/** Responsive panel geometry for the ship-console star map. */
public final class StarmapLayout
{
    public static final int BASE_CANVAS_WIDTH = StarmapGeometry.BASE_WIDTH;
    public static final int BASE_CANVAS_HEIGHT = StarmapGeometry.BASE_HEIGHT;
    private static final int WIDE_MIN_WIDTH = 620;
    private static final int WIDE_MIN_HEIGHT = 330;

    private final int panelWidth;
    private final int panelHeight;
    private final boolean compact;
    private final Bounds canvas;
    private final Bounds detail;
    private final Bounds actionButton;
    private final Bounds backButton;
    private final Bounds closeButton;
    private final StarmapViewport viewport;

    private StarmapLayout(int panelWidth, int panelHeight, boolean compact,
                          Bounds canvas, Bounds detail, Bounds actionButton,
                          Bounds backButton, Bounds closeButton, float canvasScale)
    {
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.compact = compact;
        this.canvas = canvas;
        this.detail = detail;
        this.actionButton = actionButton;
        this.backButton = backButton;
        this.closeButton = closeButton;
        this.viewport = StarmapViewport.fixed(BASE_CANVAS_WIDTH, BASE_CANVAS_HEIGHT,
                canvas.x(), canvas.y(), canvas.width(), canvas.height(), canvasScale);
    }

    public static StarmapLayout calculate(int screenWidth, int screenHeight)
    {
        int margin = screenWidth < 420 || screenHeight < 230 ? 4 : 8;
        int panelWidth = Math.max(1, screenWidth - margin * 2);
        int panelHeight = Math.max(1, screenHeight - margin * 2);
        boolean compact = panelWidth < WIDE_MIN_WIDTH || panelHeight < WIDE_MIN_HEIGHT;

        int headerHeight = panelHeight < 230 ? 38 : 46;
        int footerHeight = panelHeight < 230 ? 10 : 14;
        int contentY = Math.min(headerHeight, panelHeight);
        int contentHeight = Math.max(1, panelHeight - contentY - footerHeight);
        int inset = compact ? 6 : 10;
        int gap = compact ? 6 : 10;

        int detailWidth = compact
                ? clamp(Math.round(panelWidth * 0.42F), 156,
                        Math.min(240, Math.max(156, panelWidth - inset * 2)))
                : clamp(Math.round(panelWidth * 0.28F), 180, 280);
        detailWidth = Math.min(detailWidth, Math.max(1, panelWidth - inset * 2));
        int detailX = panelWidth - inset - detailWidth;
        Bounds detail = new Bounds(detailX, contentY, detailWidth, contentHeight);

        int mapCellWidth = compact
                ? Math.max(1, panelWidth - inset * 2)
                : Math.max(1, detailX - gap - inset);
        int mapCellHeight = contentHeight;
        float canvasScale = Math.max(0.01F, Math.min(
                mapCellWidth / (float) BASE_CANVAS_WIDTH,
                mapCellHeight / (float) BASE_CANVAS_HEIGHT));
        int canvasWidth = Math.max(1, Math.round(BASE_CANVAS_WIDTH * canvasScale));
        int canvasHeight = Math.max(1, Math.round(BASE_CANVAS_HEIGHT * canvasScale));
        int canvasX = inset + (mapCellWidth - canvasWidth) / 2;
        int canvasY = contentY + (mapCellHeight - canvasHeight) / 2;
        Bounds canvas = new Bounds(canvasX, canvasY, canvasWidth, canvasHeight);

        int actionHeight = Math.min(26, Math.max(16, detail.height() - 8));
        Bounds action = new Bounds(detail.x() + 4,
                detail.bottom() - actionHeight - 4,
                Math.max(1, detail.width() - 8), actionHeight);
        Bounds back = new Bounds(8, Math.max(18, headerHeight - 22),
                Math.min(100, Math.max(1, panelWidth - 16)), 18);
        Bounds close = new Bounds(Math.max(detail.x() + 2, detail.right() - 18),
                detail.y() + 2, 16, 16);

        return new StarmapLayout(panelWidth, panelHeight, compact, canvas, detail,
                action, back, close, canvasScale);
    }

    public int panelWidth() { return panelWidth; }
    public int panelHeight() { return panelHeight; }
    public boolean compact() { return compact; }
    public Bounds canvas() { return canvas; }
    public Bounds detail() { return detail; }
    public Bounds actionButton() { return actionButton; }
    public Bounds backButton() { return backButton; }
    public Bounds closeButton() { return closeButton; }
    public StarmapViewport viewport() { return viewport; }

    public boolean detailVisible(boolean drawerOpen)
    {
        return !compact || drawerOpen;
    }

    private static int clamp(int value, int minimum, int maximum)
    {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Bounds(int x, int y, int width, int height)
    {
        public int right() { return x + width; }
        public int bottom() { return y + height; }

        public boolean fitsInside(int outerWidth, int outerHeight)
        {
            return x >= 0 && y >= 0 && right() <= outerWidth && bottom() <= outerHeight;
        }
    }
}
