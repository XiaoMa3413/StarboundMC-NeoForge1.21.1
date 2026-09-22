# Voxel Printing Station — appearance pass

This contract covers the appearance-only pass approved on 2026-09-22. It uses the
current 320 × 240 single-panel implementation, not the older 440px chamber layout
described in `voxel-printing-station.md`.

The user subsequently accepted the in-game appearance. Its normative visual rules
are recorded in [Graphite Shipboard Fabrication Style](../ui-fabrication-style.md).
The screenshot review identified two final corrections: split tooltip line breaks
into separate styled text entries, and replace remaining stock scroll arrows.
Tooltip backgrounds also adopt the printer palette, scoped to this screen.

- **Purpose / fantasy:** operate a compact shipboard fabrication console.
- **Primary visual:** the selected item and its existing output socket; the print
  button is the primary action. Catalogue and inventory remain supporting areas.
- **Flow:** search/category → select → inspect materials → set quantity → print;
  queue management remains the existing alternate view.
- **Appearance:** graphite shell, cold white text, titanium structure, restrained
  cyan selection and interaction, amber shortages. Satisfied materials use neutral
  text instead of competing green highlights.
- **Components:** raised button edges, recessed input/slot edges, flat catalogue
  rows, solid selected-row fill, plain scrollbar thumbs and pixel category symbols.
- **States:** selection remains readable with missing materials; button edges
  reverse when pressed; disabled controls retain readable muted labels; focused
  inputs retain a cyan indicator. No idle animation.
- **Implementation:** preserve all component positions, dimensions, hit targets,
  slot coordinates, text sizes, search/quantity behavior and server semantics.
  Printer-local drawing adds edge relief inside the existing bounds. LSS owns
  fill/text colours. Generic components retain their existing behavior.
- **Validation:** the main appearance was accepted from the user's in-game screenshot.
  The tooltip/arrow corrections and the full language, GUI Scale 2/3/4 and interaction
  matrix still require in-game verification; one accepted screenshot is not a complete matrix.
