package com.starboundmc.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxelPrintingStationVisualRegressionTest {
    private static final Path MODEL = Path.of(
            "src/main/resources/assets/starboundmc/models/block/voxel_printing_station.json");
    private static final Path RENDERER = Path.of(
            "src/main/java/com/starboundmc/client/VoxelPrintingStationRenderer.java");
    private static final String[][] AXIS_FACES = {
            {"west", "east"},
            {"down", "up"},
            {"north", "south"}
    };

    @Test
    void modelHasNoPositiveVolumeOrSameFacingCoplanarOverlaps() throws IOException {
        JsonArray elements = JsonParser.parseString(Files.readString(MODEL))
                .getAsJsonObject().getAsJsonArray("elements");

        for (int firstIndex = 0; firstIndex < elements.size(); firstIndex++) {
            JsonObject first = elements.get(firstIndex).getAsJsonObject();
            for (int secondIndex = firstIndex + 1; secondIndex < elements.size(); secondIndex++) {
                JsonObject second = elements.get(secondIndex).getAsJsonObject();
                double[] overlaps = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    overlaps[axis] = overlap(first, second, axis);
                }

                assertFalse(overlaps[0] > 0.0 && overlaps[1] > 0.0 && overlaps[2] > 0.0,
                        "Elements " + firstIndex + " and " + secondIndex + " overlap in volume");

                for (int axis = 0; axis < 3; axis++) {
                    int firstOtherAxis = (axis + 1) % 3;
                    int secondOtherAxis = (axis + 2) % 3;
                    if (overlaps[firstOtherAxis] <= 0.0 || overlaps[secondOtherAxis] <= 0.0) {
                        continue;
                    }
                    for (int side = 0; side < 2; side++) {
                        String coordinate = side == 0 ? "from" : "to";
                        String face = AXIS_FACES[axis][side];
                        double firstPlane = coordinate(first, coordinate, axis);
                        double secondPlane = coordinate(second, coordinate, axis);
                        boolean bothFacesRendered = first.getAsJsonObject("faces").has(face)
                                && second.getAsJsonObject("faces").has(face);
                        assertFalse(bothFacesRendered && Math.abs(firstPlane - secondPlane) < 1.0E-9,
                                "Elements " + firstIndex + " and " + secondIndex
                                        + " render the same-facing " + face + " plane");
                    }
                }
            }
        }
    }

    @Test
    void rendererClipsAtBuildLayerAndSweepsBothInsetProbesAcrossFullWidth() throws IOException {
        String source = Files.readString(RENDERER);

        assertTrue(source.contains("pose.scale(0.32F, 0.32F, 0.32F)"));
        assertTrue(source.contains("new LayerClippedVertexConsumer"));
        assertFalse(source.contains("0.32F * formation"));
        assertTrue(source.contains("renderOverheadProbes(station, pose, buffers, scanY"));
        assertTrue(source.contains("LEFT_PROBE_PIVOT_X = 0.36F"));
        assertTrue(source.contains("RIGHT_PROBE_PIVOT_X = 0.64F"));
        assertTrue(source.contains("PROBE_PIVOT_Z = 0.50F"));
        assertTrue(source.contains("PROBE_NOZZLE_LENGTH = 0.12F"));
        assertTrue(source.contains("PROBE_FULL_SWEEP_RADIUS = 0.175F"));
        assertTrue(source.contains("interpolatedRemainingTicksAt"));
        assertTrue(source.contains("sweepOffset = Mth.sin"));
        assertTrue(source.contains("? 0.5F + sweepOffset"));
        assertTrue(source.contains("? 0.5F - sweepOffset"));
        assertTrue(source.contains("pose.mulPose(probeRotation(aim))"));
        assertTrue(source.contains("leftAim.nozzleX(), leftAim.nozzleY(), leftAim.nozzleZ()"));
        assertTrue(source.contains("rightAim.nozzleX(), rightAim.nozzleY(), rightAim.nozzleZ()"));
        assertFalse(source.contains("renderMovingProbes"));
    }

    @Test
    void rendererFinishesProbeSolidsBeforeRequestingLineBuffer() throws IOException {
        String source = Files.readString(RENDERER);
        int methodStart = source.indexOf("private void renderOverheadProbes");
        int methodEnd = source.indexOf("private static ProbeAim createProbeAim", methodStart);
        String method = source.substring(methodStart, methodEnd);

        int lastSolidDraw = method.indexOf("renderProbeSolids(pose, solids, rightAim)");
        int lineBuffer = method.indexOf("buffers.getBuffer(RenderType.lines())");
        int firstOutlineDraw = method.indexOf("renderProbeOutlines(pose, lines, leftAim)");

        assertTrue(lastSolidDraw >= 0 && lastSolidDraw < lineBuffer,
                "The debug-filled-box buffer must finish before requesting the line buffer");
        assertTrue(lineBuffer < firstOutlineDraw,
                "Probe outlines must be drawn only after the line buffer is requested");
    }

    @Test
    void singlePanelKeepsVanillaItemsInsideTheirSocketAndOutputPreviewOnTheSlot() throws IOException {
        String root = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/voxel/VoxelPrintingStationRoot.java"));
        String panel = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/SelectedItemView.java"));
        String screen = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/VoxelPrintingStationScreen.java"));
        String menu = Files.readString(Path.of(
                "src/main/java/com/starboundmc/menu/VoxelPrintingStationMenu.java"));

        // One panel size, and it is the narrowest that still fits the inventory the menu fixes.
        assertTrue(screen.contains("PANEL_W = 320"));
        assertTrue(root.contains("private static final int PANEL_W = 320"));
        // The detail region stops short of the panel edge so it cannot paint over the shell's own
        // 1px border, which is what made that border look partially missing.
        assertTrue(root.contains("private static final int WORKSPACE_W = 170"));
        assertTrue(root.contains("146 + 170 = 316"));
        assertTrue(root.contains(".width(PANEL_W)"));
        assertTrue(root.contains("voxel-inventory-section\", 144, 151, 172, 84"));
        // The output frame must sit on the vanilla slot's own coordinate, must not swallow clicks
        // meant for the slot, and must hand over to the real item when the slot is occupied.
        assertTrue(panel.contains("SOCKET_LEFT = 4"));
        assertTrue(panel.contains("SOCKET_TOP = 2"));
        String outputSlot = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/OutputPreviewSlot.java"));
        assertTrue(outputSlot.contains("SIZE = 18"));
        assertTrue(outputSlot.contains("ITEM_INSET = 1"));
        assertTrue(outputSlot.contains("setAllowHitTest(false)"));
        assertTrue(outputSlot.contains("boolean show = !occupied && !item.isEmpty()"),
                "The preview must step aside for the real item in the slot");
        assertFalse(root.contains("outputStatus"), "Output helper text must not sit under the action row");
        assertFalse(root.contains("output-status"), "Obscured output status label must be removed");
        assertTrue(root.contains("selectedItemView.setOutput(resultStack(holder), progress, running,"));
        assertTrue(menu.contains("OUTPUT_SLOT, 151, 31"));
        assertTrue(menu.contains("addPlayerInventory(inventory, 148, 161)"));
    }

    @Test
    void detailPaneUsesSemanticComponentsAndKeepsVanillaOutputSocket() throws IOException {
        String root = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/voxel/VoxelPrintingStationRoot.java"));
        String stepper = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/QuantityStepper.java"));
        String outputSlot = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/OutputPreviewSlot.java"));
        String countBar = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/PrintCountBar.java"));
        String requirement = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/RequirementRow.java"));
        String materials = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/MaterialRequirementView.java"));
        String itemHeader = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/ItemHeader.java"));
        String selectedPanel = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/SelectedItemView.java"));
        String recipeRow = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/RecipeListRow.java"));
        String browser = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/RecipeBrowser.java"));
        String search = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/RecipeSearchField.java"));
        String quantity = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/ui/components/QuantityStepper.java"));
        String stylesheet = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lss/voxel_printing_station.lss"));

        // Architectural contract: the page delegates repeated UI behaviour to the
        // semantic component layer instead of owning inline copies of it.
        assertTrue(root.contains("import com.starboundmc.client.ui.components.RecipeListRow;"));
        assertTrue(root.contains("import com.starboundmc.client.ui.components.SelectedItemView;"));
        assertTrue(root.contains("import com.starboundmc.client.ui.components.MaterialRequirementView;"));
        // The catalogue rows are built inside the browser; the page supplies entries and rules.
        assertTrue(root.contains("recipeBrowser.setEntries(entries)"));
        assertTrue(root.contains("private final SelectedItemView selectedItemView"));
        // No machine readout and no standing queue panel: the output frame and the count line carry
        // those facts now.
        assertFalse(root.contains("ProcessStrip"));
        assertFalse(root.contains("QueueSummary"));
        assertFalse(root.contains("processStrip"));
        assertFalse(root.contains("queueSummary"));

        // SelectedItemView is the composition root for the detail region: it owns the item header,
        // the material requirements, the action bar and the output socket, and exposes only the two
        // interactive children the page has to wire.
        assertTrue(selectedPanel.contains("public QuantityStepper quantityStepper()"));
        assertTrue(selectedPanel.contains("public Button craftButton()"));
        // One flex column in reading order: item identity, then materials, then the count line and
        // the action sharing a row.
        assertTrue(selectedPanel.contains("frame.addChildren(header, materials, actionRow)"));
        assertTrue(selectedPanel.contains("flexDirection(FlexDirection.COLUMN)"),
                "The view must arrange itself by flex flow, not by stacked coordinates");
        assertTrue(selectedPanel.contains("flexGrow(1)"),
                "The material view must absorb the region's leftover height");
        // The count line and the action share one row, and the button flexes into what is left.
        assertTrue(selectedPanel.contains("actionRow.addChildren(countBar, craftButton)"));
        assertTrue(selectedPanel.contains("setRequirements(List<MaterialRequirementView.Line> lines)"));
        assertTrue(selectedPanel.contains("setItem(Component itemName, int outputCount"));
        assertTrue(selectedPanel.contains("setOutput(ItemStack target, int progressPercent"));
        // The page anchors the region and wires the two controls; everything else is the panel's.
        assertTrue(root.contains("private UIElement buildWorkspacePane()"));
        assertTrue(root.contains("selectedItemView.setRequirements(lines)"));
        assertTrue(root.contains("selectedItemView.quantityStepper().onChanged("));
        assertTrue(root.contains("var craftButton = selectedItemView.craftButton();"));
        assertTrue(root.contains("craftButton.addEventListener(UIEvents.CLICK"));
        assertTrue(selectedPanel.contains("gui.starboundmc.voxel_printing.item_header.count"),
                "The yield line must be localized, not built from a literal");
        // The page no longer positions requirement rows, the item name, a status line or a
        // ghost preview itself; those are component-level concerns now.
        assertFalse(root.contains("requirementRows"));
        assertFalse(root.contains("detailStatus"));
        assertFalse(root.contains("detailOutput"));
        assertFalse(root.contains("detailDescription"));
        assertFalse(root.contains("detailName"));
        assertFalse(root.contains("ghostResultTexture"));
        assertFalse(root.contains("outputPreview"));
        assertFalse(root.contains("printButton"));

        // Telemetry-style metric panels were rejected as the wrong register for a crafting UI.
        assertFalse(root.contains("InfoSection"));
        assertFalse(root.contains("FabricationInfo"));

        // The component owns the -10/-1/+1/+10/MAX ergonomics; the page no longer does.
        assertTrue(stepper.contains("minusTen"));
        assertTrue(stepper.contains("minus"));
        assertTrue(stepper.contains("plus"));
        assertTrue(stepper.contains("plusTen"));
        assertTrue(stepper.contains("maximum"));
        // ...including the explanation for each button, and the ceiling hint when a button is
        // inert. Moving the buttons into a component must not drop their tooltips.
        assertTrue(stepper.contains("gui.starboundmc.voxel_printing.quantity.decrease_ten"));
        assertTrue(stepper.contains("gui.starboundmc.voxel_printing.quantity.increase_ten"));
        assertTrue(stepper.contains("gui.starboundmc.voxel_printing.quantity.limit"));
        assertTrue(stepper.contains("gui.starboundmc.voxel_printing.quantity.maximum"));
        // The stepper stays a compound control, and its density is its own concern: only it knows
        // how many buttons are on show, so it sets its own width and the page gives the button
        // beside it whatever is left.
        assertTrue(stepper.contains("public enum Mode"));
        assertTrue(stepper.contains("public QuantityStepper setMode(Mode value)"));

        // The view owns the row pool and the scrolling, and it is what tells its own layout to take
        // the region's leftover height; the page no longer sizes the list.
        assertTrue(materials.contains("ScrollerView"));
        assertTrue(materials.contains("RequirementRow.Mode.COMPACT"));
        assertTrue(materials.contains("setRequirements(List<Line> lines)"));
        assertTrue(root.contains("selectedItemView.setRequirements(lines)"));
        // The header states the item in words only. It must draw no icon of its own: the output
        // frame is the one place the item appears, so a second icon would be the duplication this
        // screen exists to remove.
        assertFalse(itemHeader.contains("ItemStackTexture"),
                "The header must not draw a second copy of the item");
        assertFalse(itemHeader.contains("scale("), "No oversized icon in the header");
        assertFalse(itemHeader.contains("Mth.sin"), "The header must not animate a machine");

        // The output frame is the one place the item, its progress and its pickup live.
        assertTrue(outputSlot.contains("ItemStackTexture"));
        assertTrue(outputSlot.contains("PREVIEW_ALPHA"),
                "Idle state shows a dimmed preview rather than a plain item");
        assertTrue(outputSlot.contains("enableScissor"),
                "Progress is a colour reveal, which needs a clipped draw");
        assertFalse(outputSlot.contains("ProgressBar"),
                "Progress must not be a second progress bar");
        assertTrue(outputSlot.contains("drawBackgroundAdditional"));

        // The count line is the queue representation: the stepper before a craft, the remaining
        // count during one, never both.
        assertTrue(countBar.contains("setPrinting(boolean printing"));
        assertTrue(countBar.contains("gui.starboundmc.voxel_printing.print_count.remaining"));
        assertTrue(root.contains("selectedItemView.setPrinting(running, outstanding)"));

        // The view arranges itself as a flex column rather than by stacking coordinates.
        assertTrue(selectedPanel.contains("FlexDirection.COLUMN"));
        assertTrue(selectedPanel.contains("flexGrow(1)"));

        // The browser row: the icon carries the emphasis, and selection shows as a filled
        // background plus the accent rail, with availability as its own quieter signal.
        assertTrue(recipeRow.contains("ICON = 16"));
        assertTrue(recipeRow.contains("sb-recipe-icon"));
        assertTrue(recipeRow.contains("gui.starboundmc.voxel_printing.recipe_row.yield"));
        assertTrue(stylesheet.contains(".sb-recipe-row.sb-selected:host"));
        assertTrue(stylesheet.contains(".sb-recipe-row.sb-selected .sb-recipe-rail"));

        // Density is a component concern: the row component exposes an explicit mode
        // so a page picks its density instead of the component hard-coding one. COMPACT is the
        // one that matters here: a five-material recipe has to fit without scrolling.
        assertTrue(requirement.contains("enum Mode"));
        assertTrue(requirement.contains("COMPACT(14)"));
        assertTrue(requirement.contains("NORMAL(18)"));
        assertTrue(requirement.contains("public RequirementRow setMode(Mode value)"));

        // Search is its own component and sits above the category row, so the control that reaches
        // across categories is the one that reads first.
        assertTrue(search.contains("extends TextField"));
        assertTrue(search.contains("sb-recipe-search"));
        assertTrue(search.contains("gui.starboundmc.voxel_printing.search.placeholder"));
        assertTrue(search.contains("MAX_QUERY"), "A query is bounded; an unbounded one is not a filter");
        assertTrue(root.contains("searchField.onChanged("),
                "A search edit must re-run the filter and re-check the selection");
        assertTrue(root.contains("recipeBrowser = new RecipeBrowser(128, 151 - SEARCH_ROW_H - 18)"),
                "The list gives up the room the search and category rows take");
        assertFalse(browser.contains("TextField"),
                "The browser is a list again; the search control lives above it, not inside it");
        // Search must not re-enter: the page never writes the field programmatically.
        assertFalse(root.contains("searchField.setValue"));

        // Search reaches across categories: any non-empty query runs over the whole catalogue, so a
        // result is never hidden merely because of which category happened to be selected. The
        // selector is moved to ALL at the same time, so it never shows a filter other than the one
        // actually applied.
        assertTrue(root.contains("nameMatches(index, query)"),
                "The name match must be part of the same filter as the category");
        assertTrue(root.contains("!query.isEmpty() && activeCategory != PrintingCategory.ALL"));
        assertTrue(root.contains("activeCategory = PrintingCategory.ALL"));
        assertTrue(root.contains("categorySelector.setSelected(PrintingCategory.ALL, false)"),
                "The selector must show the category that is actually applied");

        // The category list leads with ALL, and ALL matches everything while not counting as a
        // category a recipe belongs to.
        assertTrue(root.contains("PrintingCategory.ALL, PrintingCategory.SURVIVAL"));
        String category = Files.readString(Path.of(
                "src/main/java/com/starboundmc/recipe/PrintingCategory.java"));
        assertTrue(category.contains("if (this == ALL) return true;"));
        assertTrue(category.contains("public static java.util.List<PrintingCategory> concrete()"),
                "The belongs-to-exactly-one-category invariant needs the tag categories only");
        String gameTests = Files.readString(Path.of(
                "src/gameTest/java/com/starboundmc/recipe/PrintingRecipeGameTests.java"));
        assertTrue(gameTests.contains("PrintingCategory.concrete().stream()"),
                "The game test must not count ALL as a category a recipe belongs to");

        // Quantity: the count is typeable, bounded by the machine's cap and clamped to the business
        // limit, and the buttons still work.
        assertTrue(quantity.contains("private final TextField value"));
        assertTrue(quantity.contains("setNumbersOnlyInt(1, HARD_CAP)"));
        assertTrue(quantity.contains("setTextResponder"));
        assertTrue(quantity.contains("writingSelf"),
                "A self-write must not re-enter the responder, or the refresh loop would not terminate");
        assertTrue(quantity.contains("writeField("));
        assertTrue(quantity.contains("if (!value.isFocused())"),
                "The page refreshes every tick and must not overwrite a number being typed");
        assertTrue(quantity.contains("minusTen") && quantity.contains("maximum"),
                "The existing buttons stay");

        // Recipe list: rows are separated by a hairline rule instead of carrying a border.
        assertTrue(recipeRow.contains("drawBackgroundAdditional"));
        assertTrue(recipeRow.contains("RULE_COLOR"));
        // The row keeps no copy of its name: the page reads it from the recipe stack when filtering,
        // so there is one source for what the item is called.
        assertFalse(recipeRow.contains("displayName"));

        // The blocking reason travels on the action button, which is the control the player is
        // reaching for.
        assertTrue(root.contains("craftButton().style(style -> style.tooltips(reasonTooltip))"),
                "A blocked action must explain itself on the control the player is reaching for");

        // The world renderer owns fabrication spectacle; no GUI fabrication canvas remains.
        assertFalse(root.contains("FabricationCanvas"));
        assertFalse(root.contains("fabricationTexture"));
        assertFalse(root.contains("fabricationPreview"));

        // The output frame is mounted by the view on the coordinate its menu slot dictates, and the
        // frame itself is the component that owns the socket class and the dimmed preview tint.
        assertTrue(selectedPanel.contains("SOCKET_LEFT = 4"));
        assertTrue(selectedPanel.contains("SOCKET_TOP = 2"));
        assertTrue(selectedPanel.contains("new OutputPreviewSlot()"));
        assertTrue(outputSlot.contains("\"voxel-printing-output-socket\""));
        // The preview must be faint and washed toward neutral, so it never reads as a real item
        // sitting in the slot; the reveal paints over the wash and restores full colour.
        assertTrue(outputSlot.contains("PREVIEW_ALPHA = 0x59FFFFFF"));
        assertTrue(outputSlot.contains("PREVIEW_WASH"));
        int wash = outputSlot.indexOf("PREVIEW_WASH");
        int reveal = outputSlot.indexOf("reveal.draw(");
        assertTrue(wash > 0 && reveal > wash,
                "The wash must be applied before the coloured reveal, not after it");

        // The item's own description lines are still surfaced, now on the header's tooltip.
        assertTrue(root.contains("itemDescription(result)"));
        assertTrue(root.contains("selectedItemView.setItem("));

        // Component styles are page-local .sb-* rules; the old per-card chrome is gone.
        assertTrue(stylesheet.contains(".sb-requirement-row"));
        assertTrue(stylesheet.contains(".sb-quantity-stepper"));
        assertTrue(stylesheet.contains(".sb-item-header"));
        assertTrue(stylesheet.contains(".sb-item-name"));
        assertTrue(stylesheet.contains(".sb-craft-action-bar"));
        assertTrue(stylesheet.contains(".sb-output-slot"));
        assertTrue(stylesheet.contains(".sb-recipe-search"));
        assertTrue(stylesheet.contains(".sb-quantity-value:host"));
        assertTrue(stylesheet.contains(".sb-print-count-bar"));
        assertFalse(stylesheet.contains(".voxel-fabrication-chamber"));
        assertFalse(stylesheet.contains(".voxel-requirement-card"));
        // The retired readouts must not linger: the output frame and the count line carry their facts.
        assertFalse(stylesheet.contains(".sb-process-strip"));
        assertFalse(stylesheet.contains(".sb-queue-summary"));
        assertFalse(stylesheet.contains(".sb-item-preview"));
        // The retired telemetry panel's rules must not linger either.
        assertFalse(stylesheet.contains(".sb-info-section"));
        assertFalse(stylesheet.contains(".sb-info-value"));
        assertFalse(stylesheet.contains(".voxel-printing-detail-name"));

        // Requirement rows stay flat data lines: no per-row panel background survives.
        assertTrue(stylesheet.replace("\r\n", "\n")
                .contains(".sb-requirement-row {\n  background: rect(#00000000);\n}"));

        // The v1.1 overlay proposed text-transform and a `transparent` keyword; LDLib2 2.2.36.a
        // has neither (no such property, and no such texture expression), so a stylesheet
        // carrying them would only log parse noise.
        assertFalse(stylesheet.contains("text-transform"));
        assertFalse(stylesheet.contains("transparent"));
    }

    private static double overlap(JsonObject first, JsonObject second, int axis) {
        double lower = Math.max(coordinate(first, "from", axis), coordinate(second, "from", axis));
        double upper = Math.min(coordinate(first, "to", axis), coordinate(second, "to", axis));
        return upper - lower;
    }

    private static double coordinate(JsonObject element, String endpoint, int axis) {
        return element.getAsJsonArray(endpoint).get(axis).getAsDouble();
    }
}
