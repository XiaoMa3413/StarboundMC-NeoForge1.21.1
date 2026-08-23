package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarmapVisualThemeTest
{
    @Test
    void preservesCurrentColorPalette()
    {
        assertEquals(0xFF090D14, StarmapVisualTheme.BACKGROUND_TOP);
        assertEquals(0xFF030508, StarmapVisualTheme.BACKGROUND_BOTTOM);
        assertEquals(0xFF04070B, StarmapVisualTheme.SHELL_SHADOW);
        assertEquals(0xFF151C25, StarmapVisualTheme.SHELL_SURFACE);
        assertEquals(0xFF202A35, StarmapVisualTheme.SHELL_RAISED);
        assertEquals(0xFF334353, StarmapVisualTheme.SHELL_EDGE);
        assertEquals(0xFF526678, StarmapVisualTheme.SHELL_HIGHLIGHT);
        assertEquals(0xFF26333F, StarmapVisualTheme.SHELL_SEAM);
        assertEquals(0xFF0F151D, StarmapVisualTheme.HEADER_SURFACE);
        assertEquals(0xFF070A0F, StarmapVisualTheme.FRAME_INNER);
        assertEquals(0xF20B1017, StarmapVisualTheme.DETAIL_SURFACE);
        assertEquals(0xFF0B1017, StarmapVisualTheme.DETAIL_SURFACE_OPAQUE);
        assertEquals(0xFF05080C, StarmapVisualTheme.DISPLAY_BEZEL);
        assertEquals(0xFF263F4A, StarmapVisualTheme.DISPLAY_EDGE);
        assertEquals(0xFF080D17, StarmapVisualTheme.DISPLAY_TOP);
        assertEquals(0xFF03060B, StarmapVisualTheme.DISPLAY_BOTTOM);
        assertEquals(0xFF3FC4D4, StarmapVisualTheme.ACCENT);
        assertEquals(0xFF1D6874, StarmapVisualTheme.ACCENT_DIM);
        assertEquals(0xFF4A5C6D, StarmapVisualTheme.MECHANICAL_MARK);
        assertEquals(0xFF54DFD8, StarmapVisualTheme.SELECTION);
        assertEquals(0x9A79AAB2, StarmapVisualTheme.HOVER_RING);
        assertEquals(0x88458A96, StarmapVisualTheme.CANVAS_CALIBRATION);
        assertEquals(0x0096AFC9, StarmapVisualTheme.CANVAS_STAR_RGB);
        assertEquals(0x006E98A3, StarmapVisualTheme.ORBIT_MAJOR_RGB);
        assertEquals(0x0080AEB5, StarmapVisualTheme.ORBIT_MINOR_RGB);
        assertEquals(0xFF40E0C0, StarmapVisualTheme.STATUS_CURRENT);
        assertEquals(0xFF60E0A0, StarmapVisualTheme.STATUS_VISITED);
        assertEquals(0xFFE0A84B, StarmapVisualTheme.STATUS_ATTENTION);
        assertEquals(0xFFE06060, StarmapVisualTheme.STATUS_DANGER);
        assertEquals(0xFFE0A84B, StarmapVisualTheme.STATUS_FUEL);
        assertEquals(0x88E08060, StarmapVisualTheme.STATUS_RADIATION);
        assertEquals(0xFFFFFFFF, StarmapVisualTheme.TEXT_PRIMARY);
        assertEquals(0xFFE8E8E8, StarmapVisualTheme.TEXT_STANDARD);
        assertEquals(0xFF9FB4C8, StarmapVisualTheme.TEXT_SECONDARY);
        assertEquals(0xFFC8C8D8, StarmapVisualTheme.TEXT_BODY);
        assertEquals(0xFF808088, StarmapVisualTheme.TEXT_DISABLED);
        assertEquals(0xE0101821, StarmapVisualTheme.DETAIL_HEADER_OVERLAY);
        assertEquals(0x88334F5B, StarmapVisualTheme.DETAIL_DIVIDER);
        assertEquals(0xA6080E14, StarmapVisualTheme.DETAIL_DESCRIPTION_SURFACE);
        assertEquals(0xD00D141C, StarmapVisualTheme.DETAIL_SECTION_SURFACE);
        assertEquals(0xFF263F4A, StarmapVisualTheme.DETAIL_SECTION_EDGE);
        assertEquals(0xFF173541, StarmapVisualTheme.DETAIL_LABEL_SURFACE);
        assertEquals(0xFF070A0F, StarmapVisualTheme.FUEL_TRACK);
        assertEquals(0xFF66502D, StarmapVisualTheme.FUEL_EDGE);
        assertEquals(0xAA15100A, StarmapVisualTheme.FUEL_SEPARATOR);
        assertEquals(0xFF11161C, StarmapVisualTheme.BUTTON_FILL_DISABLED);
        assertEquals(0xFF18242D, StarmapVisualTheme.BUTTON_FILL_NORMAL);
        assertEquals(0xFF1C303A, StarmapVisualTheme.BUTTON_FILL_FOCUSED);
        assertEquals(0xFF21434D, StarmapVisualTheme.BUTTON_FILL_HOVERED);
        assertEquals(0xFF173C43, StarmapVisualTheme.BUTTON_PRIMARY_FILL);
        assertEquals(0xFF1C5660, StarmapVisualTheme.BUTTON_PRIMARY_HOVERED);
        assertEquals(0xFF2A5962, StarmapVisualTheme.BUTTON_STATUS_DIM);
        assertEquals(0xA040E0C0, StarmapVisualTheme.ROUTE_LINE);
        assertEquals(0x0040E0C0, StarmapVisualTheme.SHIP_RGB);
        assertEquals(0x00E8A040, StarmapVisualTheme.SHIP_FLAME_RGB);
        assertEquals(0xFF40E0C0, StarmapVisualTheme.ROUTE_SHIP);
        assertEquals(0xFFE8FFFF, StarmapVisualTheme.ROUTE_SHIP_HIGHLIGHT);
    }

    @Test
    void preservesCurrentMechanicalScale()
    {
        assertEquals(1, StarmapVisualTheme.FRAME_STROKE_VIRTUAL);
        assertEquals(5, StarmapVisualTheme.CUT_CORNER_SIZE);
        assertEquals(1, StarmapVisualTheme.FRAME_LAYER_WIDTH);
        assertEquals(2, StarmapVisualTheme.DETAIL_ACCENT_WIDTH);
        assertEquals(2, StarmapVisualTheme.SHELL_INSET);
        assertEquals(4, StarmapVisualTheme.DISPLAY_BEZEL_WIDTH);
        assertEquals(2, StarmapVisualTheme.FUEL_SLOT_PADDING);
        assertEquals(16, StarmapVisualTheme.FUEL_SEGMENT_LENGTH);
        assertEquals(13, StarmapVisualTheme.TITLE_PLATE_HEIGHT);
        assertEquals(72, StarmapVisualTheme.TITLE_PLATE_MIN_WIDTH);
        assertEquals(132, StarmapVisualTheme.TITLE_PLATE_MAX_WIDTH);
        assertEquals(7, StarmapVisualTheme.DETAIL_CONTENT_INSET);
        assertEquals(28, StarmapVisualTheme.DETAIL_HEADER_HEIGHT);
        assertEquals(20, StarmapVisualTheme.DETAIL_HINT_HEIGHT);
        assertEquals(5, StarmapVisualTheme.DETAIL_ACTION_CLEARANCE);
        assertEquals(9, StarmapVisualTheme.DETAIL_DESCRIPTION_LINE_HEIGHT);
        assertEquals(13, StarmapVisualTheme.DETAIL_SECTION_MIN_HEIGHT);
        assertEquals(10, StarmapVisualTheme.DETAIL_LINE_HEIGHT);
        assertEquals(26, StarmapVisualTheme.DETAIL_LABEL_MIN_WIDTH);
        assertEquals(42, StarmapVisualTheme.DETAIL_LABEL_MAX_WIDTH);
        assertEquals(8, StarmapVisualTheme.LABEL_CAP_HEIGHT);
        assertEquals(4, StarmapVisualTheme.TICK_LENGTH);
        assertEquals(1, StarmapVisualTheme.FASTENER_SIZE);
        assertEquals(2, StarmapVisualTheme.SECTION_GAP);
        assertEquals(4, StarmapVisualTheme.RAIL_SEGMENT_LENGTH);
        assertEquals(3, StarmapVisualTheme.RAIL_SEGMENT_GAP);
        assertEquals(8, StarmapVisualTheme.CORNER_BRACKET_MIN_SIZE);
        assertEquals(3, StarmapVisualTheme.CORNER_BRACKET_MIN_LENGTH);
        assertEquals(6, StarmapVisualTheme.CORNER_BRACKET_MAX_LENGTH);
        assertEquals(8, StarmapVisualTheme.CORNER_BRACKET_LENGTH_DIVISOR);
        assertEquals(0x86, StarmapVisualTheme.SELECTION_ALPHA_MIN);
        assertEquals(0xB4, StarmapVisualTheme.SELECTION_ALPHA_MAX);
        assertEquals(72, StarmapVisualTheme.CANVAS_BACKGROUND_STAR_COUNT);
        assertEquals(0x2A, StarmapVisualTheme.CANVAS_STAR_ALPHA_MIN);
        assertEquals(0x62, StarmapVisualTheme.CANVAS_STAR_ALPHA_MAX);
        assertEquals(0x62, StarmapVisualTheme.ORBIT_MAJOR_ALPHA);
        assertEquals(0x78, StarmapVisualTheme.ORBIT_MINOR_ALPHA);
        assertEquals(4, StarmapVisualTheme.CANVAS_CALIBRATION_LONG);
        assertEquals(2, StarmapVisualTheme.CANVAS_CALIBRATION_SHORT);
        assertEquals(1, StarmapVisualTheme.BUTTON_PRESS_OFFSET);
    }
}
