# Matter Manipulator Upgrade UI — V2 screenshot review

## Screenshot findings

- The Matter Manipulator now reads as the primary visual. Preserve this.
- Numbered 1/2/3 nodes still read like an RPG target-level tree even though every node only selects the same subsystem.
- The node tooltip obscures the primary visual and duplicates information already available in the status strip.
- The full rectangular focus frame around the tool adds another visible nested box.

## V2 corrections

- Replace numbered level buttons with small calibration/progress pips.
- Keep pips clickable only as a generous subsystem-selection target; they no longer imply jump-to-level behavior.
- Show `current/max` in the subsystem label.
- Distinguish completed, next-but-underfunded, ready, future-locked, and maxed states.
- Remove per-pip tooltips.
- Replace the full tool focus rectangle with restrained corner registration brackets.
- Preserve the 320x250 shell, real menu slot coordinates, network packet, upgrade costs, and server upgrade semantics.
