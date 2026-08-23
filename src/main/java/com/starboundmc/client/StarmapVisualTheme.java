package com.starboundmc.client;

/**
 * Semantic colors and mechanical dimensions shared by the star-map UI.
 *
 * <p>Celestial colors intentionally do not belong here: the theme controls the
 * navigation terminal around them, not the appearance of stars and planets.</p>
 */
public final class StarmapVisualTheme
{
    public static final int BACKGROUND_TOP = 0xFF090D14;
    public static final int BACKGROUND_BOTTOM = 0xFF030508;
    public static final int SHELL_SHADOW = 0xFF04070B;
    public static final int SHELL_SURFACE = 0xFF151C25;
    public static final int SHELL_RAISED = 0xFF202A35;
    public static final int SHELL_EDGE = 0xFF334353;
    public static final int SHELL_HIGHLIGHT = 0xFF526678;
    public static final int SHELL_SEAM = 0xFF26333F;
    public static final int HEADER_SURFACE = 0xFF0F151D;
    public static final int FRAME_INNER = 0xFF070A0F;
    public static final int DETAIL_SURFACE = 0xF20B1017;
    public static final int DETAIL_SURFACE_OPAQUE = 0xFF0B1017;
    public static final int DISPLAY_BEZEL = 0xFF05080C;
    public static final int DISPLAY_EDGE = 0xFF263F4A;
    public static final int DISPLAY_TOP = 0xFF080D17;
    public static final int DISPLAY_BOTTOM = 0xFF03060B;

    public static final int ACCENT = 0xFF3FC4D4;
    public static final int ACCENT_DIM = 0xFF1D6874;
    public static final int MECHANICAL_MARK = 0xFF4A5C6D;
    public static final int SELECTION = 0xFF54DFD8;
    public static final int HOVER_RING = 0x9A79AAB2;
    public static final int CANVAS_CALIBRATION = 0x88458A96;
    public static final int CANVAS_STAR_RGB = 0x0096AFC9;
    public static final int ORBIT_MAJOR_RGB = 0x006E98A3;
    public static final int ORBIT_MINOR_RGB = 0x0080AEB5;
    public static final int STATUS_CURRENT = 0xFF40E0C0;
    public static final int STATUS_VISITED = 0xFF60E0A0;
    public static final int STATUS_ATTENTION = 0xFFE0A84B;
    public static final int STATUS_DANGER = 0xFFE06060;
    public static final int STATUS_FUEL = 0xFFE0A84B;
    public static final int STATUS_RADIATION = 0x88E08060;

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_STANDARD = 0xFFE8E8E8;
    public static final int TEXT_SECONDARY = 0xFF9FB4C8;
    public static final int TEXT_BODY = 0xFFC8C8D8;
    public static final int TEXT_DISABLED = 0xFF808088;

    public static final int DETAIL_HEADER_OVERLAY = 0xE0101821;
    public static final int DETAIL_DIVIDER = 0x88334F5B;
    public static final int DETAIL_DESCRIPTION_SURFACE = 0xA6080E14;
    public static final int DETAIL_SECTION_SURFACE = 0xD00D141C;
    public static final int DETAIL_SECTION_EDGE = 0xFF263F4A;
    public static final int DETAIL_LABEL_SURFACE = 0xFF173541;

    public static final int FUEL_TRACK = 0xFF070A0F;
    public static final int FUEL_EDGE = 0xFF66502D;
    public static final int FUEL_SEPARATOR = 0xAA15100A;
    public static final int BUTTON_FILL_DISABLED = 0xFF11161C;
    public static final int BUTTON_FILL_NORMAL = 0xFF18242D;
    public static final int BUTTON_FILL_FOCUSED = 0xFF1C303A;
    public static final int BUTTON_FILL_HOVERED = 0xFF21434D;
    public static final int BUTTON_PRIMARY_FILL = 0xFF173C43;
    public static final int BUTTON_PRIMARY_HOVERED = 0xFF1C5660;
    public static final int BUTTON_STATUS_DIM = 0xFF2A5962;

    public static final int ROUTE_LINE = 0xA040E0C0;
    public static final int SHIP_RGB = 0x0040E0C0;
    public static final int SHIP_FLAME_RGB = 0x00E8A040;
    public static final int ROUTE_SHIP = 0xFF40E0C0;
    public static final int ROUTE_SHIP_HIGHLIGHT = 0xFFE8FFFF;

    public static final int FRAME_STROKE_VIRTUAL = 1;
    public static final int CUT_CORNER_SIZE = 5;
    public static final int FRAME_LAYER_WIDTH = 1;
    public static final int DETAIL_ACCENT_WIDTH = 2;
    public static final int SHELL_INSET = 2;
    public static final int DISPLAY_BEZEL_WIDTH = 4;
    public static final int FUEL_SLOT_PADDING = 2;
    public static final int FUEL_SEGMENT_LENGTH = 16;
    public static final int TITLE_PLATE_HEIGHT = 13;
    public static final int TITLE_PLATE_MIN_WIDTH = 72;
    public static final int TITLE_PLATE_MAX_WIDTH = 132;
    public static final int DETAIL_CONTENT_INSET = 7;
    public static final int DETAIL_HEADER_HEIGHT = 28;
    public static final int DETAIL_HINT_HEIGHT = 20;
    public static final int DETAIL_ACTION_CLEARANCE = 5;
    public static final int DETAIL_DESCRIPTION_LINE_HEIGHT = 9;
    public static final int DETAIL_SECTION_MIN_HEIGHT = 13;
    public static final int DETAIL_LINE_HEIGHT = 10;
    public static final int DETAIL_LABEL_MIN_WIDTH = 26;
    public static final int DETAIL_LABEL_MAX_WIDTH = 42;
    public static final int LABEL_CAP_HEIGHT = 8;
    public static final int TICK_LENGTH = 4;
    public static final int FASTENER_SIZE = 1;
    public static final int SECTION_GAP = 2;
    public static final int RAIL_SEGMENT_LENGTH = 4;
    public static final int RAIL_SEGMENT_GAP = 3;
    public static final int CORNER_BRACKET_MIN_SIZE = 8;
    public static final int CORNER_BRACKET_MIN_LENGTH = 3;
    public static final int CORNER_BRACKET_MAX_LENGTH = 6;
    public static final int CORNER_BRACKET_LENGTH_DIVISOR = 8;
    public static final int SELECTION_ALPHA_MIN = 0x86;
    public static final int SELECTION_ALPHA_MAX = 0xB4;
    public static final int CANVAS_BACKGROUND_STAR_COUNT = 72;
    public static final int CANVAS_STAR_ALPHA_MIN = 0x2A;
    public static final int CANVAS_STAR_ALPHA_MAX = 0x62;
    public static final int ORBIT_MAJOR_ALPHA = 0x62;
    public static final int ORBIT_MINOR_ALPHA = 0x78;
    public static final int CANVAS_CALIBRATION_LONG = 4;
    public static final int CANVAS_CALIBRATION_SHORT = 2;
    public static final int BUTTON_PRESS_OFFSET = 1;

    private StarmapVisualTheme() {}
}
