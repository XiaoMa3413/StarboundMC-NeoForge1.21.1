# Matter Manipulator Upgrade UI — V3 polish review

V2 established the correct structure. V3 deliberately avoids another layout rewrite and only tightens hierarchy.

## Screenshot findings

- The Matter Manipulator is now the primary visual and should remain so.
- The four subsystem branches read as engineering connections rather than an RPG skill tree.
- The selected connector was too bright/thick and competed with the tool silhouette.
- Branch labels duplicated exact level information already present in the detail strip.
- The detail-strip cost and slot-label bounds overlapped and needed safer spacing.
- The APPLY control can remain large enough for interaction, but should not become a brighter focal point than the equipment viewport.

## V3 changes

- Selected connector is now a restrained engineering green at the same line weight as other connectors.
- Branch labels show subsystem identity only; exact `current / max` stays in the selected-detail strip.
- Detail-strip bounds are adjusted to avoid text overlap.
- APPLY width is reduced slightly to read more like a console control.
- Selected pip borders are softened so selection guides the eye without overpowering the blueprint.

No menu, network, cost, level, or server behavior changes.
