# HUD / AR H0 Visual Baseline

Baseline commit: `5a3cf53` (`origin/main`, 2026-09-19 checkout)

This inventory records behavior observed in the current implementation before the H1/H2
refactor. It is a regression contract, not a replacement design specification.

## Visor / EVA

- `OxygenHudLayer` is active for a living player while the GUI is visible.
- The EVA navigation group appears only in the ship dimension while EVA mode is not `NORMAL`.
- The top group contains the cyan compass, numeric heading and EVA control/recall hint.
- A bounded camera-lag offset applies to visor content only. It resets across pauses, menus,
  camera changes, large camera cuts and long frame gaps.
- Ship and relay markers do not receive that lag.

## World AR

- The ship marker is anchored one block above `ShipStructure.SHIP_TELEPORTER_POS`.
- The relay marker is anchored at `RelayGeometry.center(snapshot.origin())` while the relay client
  state is local/active.
- World positions are transformed by the real captured View/Projection matrices.
- Visible targets use an open diamond. Off-screen and behind-camera targets use an edge chevron;
  behind-camera labels retain the localized `behind` suffix.
- Ship cyan is `#95E8E2`; relay amber is `#FFD17C`. Labels retain distance and height.

## Survival telemetry

- O2 fades in for vacuum, exposure or refill, holds after safety returns, then fades out.
- Cold and heat rows retain their existing exposure/protection rules and independent fades.
- Warning colors, pulse timing, localized captions, bar fill and row stacking are unchanged.

## N.O.V.A. broadcast

- `NovaBroadcastHudLayer` remains an independent lower-left LDLib2 layer.
- Its opening/streaming/holding/closing timeline, portrait activity, typewriter sound cadence,
  pause behavior and chat-history handoff remain unchanged.
- It is a flat communication layer: no visor projection and no visor drift.

## Manual capture still required

Screenshots and motion comparison require a running client. Capture the same EVA view at 16:9
and ultrawide, GUI scales 1 and 4, and at two FOV values, including an overlapping Ship/Relay
case and active O2/cold/heat rows. Start a development client with
`./gradlew runClient -PhudCalibrationGrid` to show the visor optical calibration grid; normal runs leave it
disabled.
