"""Distinct silhouettes for the four native humanoid equipment sets (model pixels)."""


def armor_geometry(profile, inner):
    parts = {n: [] for n in ('head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg')}

    def box(part, name, xyz, size, material, angle=0):
        cube = {'name': name, 'from': xyz, 'size': size, 'material': material}
        if angle:
            cube['rotation'] = {'origin': [xyz[0] + size[0] / 2, xyz[1] + size[1] / 2, xyz[2]], 'axis': 'z', 'angle': angle}
        parts[part].append(cube)

    def rivets(part, x, y, z, count, step=1):
        for i in range(count): box(part, 'recessed_fastener', [x + i * step, y, z], [.3, .3, .18], 3)

    def slats(part, x, y, z, count, width, material=1):
        for i in range(count): box(part, 'vent_slat', [x, y + i * .65, z], [width, .25, .22], material)

    if inner:
        box('body', 'waist_seal', [-4.35, 9.4, -2.35], [8.7, 2.8, 4.7], 1)
        for leg, side in [('right_leg', -1), ('left_leg', 1)]:
            box(leg, 'joint_undersuit', [-2.34, -.2, -2.34], [4.68, 11.8, 4.68], 1)
            if profile == 'pioneer':
                box(leg, 'canvas_thigh', [-2.36, .4, -2.4], [4.72, 4.1, 4.8], 2)
                box(leg, 'cargo_pocket', [-3.2 if side < 0 else 2.3, 1.2, -1.6], [.9, 3.5, 3.2], 1)
                box(leg, 'knee_strap', [-2.45, 5.3, -2.45], [4.9, .6, 4.9], 5)
                box(leg, 'knee_pad', [-1.9, 5.2, -2.95], [3.8, 2.5, .7], 3)
                box(leg, 'knee_pad_inset', [-1.4, 5.6, -3.1], [2.8, 1.7, .2], 1)
            elif profile == 'vanguard':
                for y, w in ((.5, 5.2), (2.8, 5), (5.1, 4.8)):
                    box(leg, 'overlapping_leg_lamella', [-w / 2, y, -3.1], [w, 2, 1.2], 2)
                    box(leg, 'lamella_lip', [-w / 2, y + 1.7, -3.25], [w, .3, .3], 3)
                box(leg, 'knee_bastion', [-2.65, 6, -3.7], [5.3, 2.3, 1.6], 3)
                box(leg, 'knee_orange_inset', [-1.7, 6.4, -3.85], [3.4, .45, .2], 6)
                box(leg, 'outer_leg_brace', [-2.9 if side < 0 else 2.35, .5, -.8], [.55, 8, 1.6], 3)
            elif profile == 'ranger':
                box(leg, 'tapered_thigh', [-1.7, .7, -2.65], [3.4, 3.4, .5], 2)
                box(leg, 'thigh_strap', [-2.4, 3.8, -2.4], [4.8, .5, 4.8], 5)
                box(leg, 'floating_kneecap', [-1.5, 5.6, -2.9], [3, 1.9, .6], 3, 12 * side)
                box(leg, 'side_equipment_case', [-3.1 if side < 0 else 2.35, 1.2, -.9], [.75, 3.2, 1.8], 2)
                box(leg, 'tracking_strip', [-.8, 1, -2.85], [.3, 2.4, .2], 6)
            else:
                for y in (.5, 2.7, 4.9):
                    box(leg, 'faceted_leg_plate', [-2.3, y, -3], [4.6, 1.9, .8], 3, side * 8)
                    box(leg, 'leg_energy_seam', [-1.6, y + 1.6, -3.2], [3.2, .22, .2], 6)
                box(leg, 'diamond_knee', [-1.25, 6.3, -3.4], [2.5, 2.5, .8], 4, 45)
                box(leg, 'knee_core', [-.55, 7, -3.6], [1.1, 1.1, .25], 6, 45)
            # A visible dark seam between left/right plates, even in an idle narrow stance.
            for cube in parts[leg]:
                if cube['from'][2] < -2.6 and cube['size'][0] > 3.5:
                    cube['from'][0] = -1.75; cube['size'][0] = 3.5
        return parts

    # Common flexible seal only. All visible hard shells are individually designed.
    box('body', 'torso_seal', [-4.34, -.3, -2.34], [8.68, 12.6, 4.68], 1)
    box('head', 'helmet_seal', [-4.2, -8.2, -4.2], [8.4, 8.4, 8.4], 1)
    if profile == 'pioneer':
        box('head', 'mining_cap', [-4.55, -8.65, -4.5], [9.1, 3, 9], 2)
        box('head', 'projecting_hat_brim', [-4.9, -6.1, -5.7], [9.8, .65, 10.2], 3)
        box('head', 'goggle_frame', [-3.6, -5.3, -4.5], [7.2, 2.6, .55], 0)
        for x in (-3.25, .3): box('head', 'amber_goggle', [x, -4.95, -4.7], [2.95, 1.65, .25], 5)
        box('head', 'respirator', [-2.4, -2.6, -5], [4.8, 2.5, 1], 2)
        slats('head', -1.6, -2.1, -5.2, 3, 3.2)
        for x in (-4.6, 3.2): box('head', 'filter_cartridge', [x, -3, -4.6], [1.4, 2.4, 1.8], 3)
        box('head', 'lamp_mount', [-1.4, -8.9, -5], [2.8, 2.7, 1], 1)
        box('head', 'work_lamp', [-1, -8.5, -5.3], [2, 1.9, .35], 6)
        box('head', 'lamp_center', [-.55, -8.1, -5.45], [1.1, 1.1, .2], 7)
        for x in (-3.4, 2.8): box('head', 'cap_ridge', [x, -8.95, -3.5], [.6, .35, 7], 3)
        for x in (-3.7, 2.7): box('body', 'utility_harness', [x, .3, -2.65], [1, 9.8, .4], 5)
        box('body', 'work_bib', [-2.5, 2.5, -2.7], [5, 4.5, .5], 2)
        box('body', 'chest_id', [-2.1, 3, -3.25], [2.4, .55, .2], 6)
        for x in (-3.8, .5):
            box('body', 'tool_pouch', [x, 7, -3.4], [3.3, 3, 1.1], 2)
            box('body', 'pouch_flap', [x, 7, -3.65], [3.3, .65, .3], 3)
        box('body', 'rear_harness_crossbar', [-3.7, 3, 2.4], [7.4, .8, .4], 5)
        box('body', 'rear_waist_tool_roll', [-3.5, 9, 2.4], [7, 2, .8], 2)
    elif profile == 'vanguard':
        box('head', 'siege_helmet_top', [-4.65, -8.8, -4.6], [9.3, 3.2, 9.2], 2)
        box('head', 'rear_helmet_armor', [-4.6, -6, 1.8], [9.2, 6.4, 2.8], 2)
        box('head', 'deep_t_visor', [-3.8, -5.7, -4.55], [7.6, 5.4, .4], 0)
        for x in (-4.5, 1):
            box('head', 'angular_cheek_guard', [x, -4.5, -5], [3.5, 4.8, 1], 3)
            slats('head', x + .5, -2.9, -5.2, 3, 2.3)
        box('head', 'brow_wedge', [-4.7, -6.6, -5.1], [9.4, 1, 1], 3)
        box('head', 'orange_crest', [-.8, -9.2, -4.7], [1.6, .5, 8.6], 6)
        box('body', 'rear_gorget', [-5.2, -1.9, 1.8], [10.4, 4, 1.5], 3)
        for x in (-5.2, 3.9): box('body', 'side_gorget', [x, -1.5, -2.8], [1.3, 3.3, 4.6], 2)
        for x, angle in ((-4.6, -8), (.25, 8)):
            box('body', 'heavy_chest_block', [x, 1, -3.7], [4.35, 4.2, 1.8], 3, angle)
            box('body', 'chest_dark_inset', [x + .4, 2, -4], [3.55, 2, .4], 2, angle)
            rivets('body', x + .5, 1.4, -4.15, 3, 1.25)
        for y in (5.5, 7.6, 9.7): box('body', 'abdomen_lamella', [-3.5, y, -3.3], [7, 1.8, 1], 2)
        box('body', 'abdomen_rank', [-1.2, 6, -3.5], [2.4, .5, .25], 6)
        for x in (-4.2, 2.6): box('body', 'back_support_beam', [x, .5, 2.4], [1.6, 10.6, .8], 3)
        slats('body', -2, 2, 2.55, 8, 4)
    elif profile == 'ranger':
        box('head', 'low_sensor_cap', [-4.35, -8.45, -4.25], [8.7, 2.5, 8.5], 2)
        box('head', 'wraparound_visor', [-4.35, -5.95, -4.5], [8.7, 1.8, .4], 0)
        box('head', 'narrow_sensor_glass', [-3.9, -5.5, -4.8], [7.8, .6, .35], 6)
        box('head', 'lower_mask', [-3.7, -3.8, -4.45], [7.4, 3.5, .55], 2)
        slats('head', -1.5, -2.9, -4.7, 3, 3)
        box('head', 'left_rangefinder', [-5, -5.8, -2.9], [.85, 2.3, 3.5], 3)
        box('head', 'rangefinder_lens', [-4.9, -5.3, -3.2], [.65, 1.2, .35], 6)
        box('head', 'short_antenna', [-4.8, -9.9, .3], [.35, 4.1, .4], 1)
        box('head', 'antenna_tip', [-4.85, -10.1, .25], [.45, .6, .5], 6)
        box('body', 'light_sternum_plate', [-2.9, 1.4, -2.7], [5.8, 4.4, .6], 2)
        box('body', 'diagonal_sling', [-.5, .2, -3.15], [1, 10.2, .3], 5, 30)
        box('body', 'sling_buckle', [-1.2, 4.4, -3.45], [2.4, 1.2, .35], 3, 30)
        box('body', 'side_magazine_case', [-4.3, 7.3, -3.05], [2.3, 3.4, .7], 2)
        for x in (1.2, 2.7): box('body', 'belt_cell', [x, 8.5, -2.9], [.9, 2.2, .6], 3)
        for y in (1, 3.5, 6): box('body', 'rear_spine_segment', [-1.4, y, 2.4], [2.8, 1.7, .5], 3)
    else:
        box('head', 'crown_shell', [-4.5, -8.8, -4.4], [9, 8.8, 8.8], 2)
        box('head', 'black_mask_recess', [-3.8, -6.1, -4.65], [7.6, 5, .35], 0)
        box('head', 'luminous_visor', [-3.6, -4.9, -5], [7.2, .65, .45], 7)
        for x, angle in ((-4.4, -18), (2.6, 18)):
            box('head', 'crown_fin', [x, -10.2, -3.4], [1.8, 7.7, 1.6], 4, angle)
            box('head', 'cheek_facet', [x, -3.5, -5], [1.8, 3.5, .8], 3, -angle)
        box('head', 'crown_keystone', [-.8, -9.5, -4.9], [1.6, 3, .8], 6)
        box('head', 'chin_keystone', [-1, -2, -4.85], [2, 2.6, .7], 4)
        for x, angle in ((-4.1, -16), (.3, 16)):
            box('body', 'swept_breastplate', [x, .8, -3.2], [3.8, 4.3, 1.1], 4, angle)
        box('body', 'core_frame', [-1.8, 2.4, -3.75], [3.6, 3.6, .8], 5, 45)
        box('body', 'starcore', [-1.2, 3, -4], [2.4, 2.4, .4], 6, 45)
        box('body', 'core_light', [-.5, 3.7, -4.2], [1, 1, .25], 7, 45)
        for y in (6.5, 8.3):
            box('body', 'abdominal_facet', [-3.2, y, -2.9], [6.4, 1.5, .7], 3)
            box('body', 'abdominal_energy_seam', [-2.6, y + 1.3, -3.1], [5.2, .18, .25], 6)
        for x, angle in ((-5, 12), (2.1, -12)):
            for y in (9.7, 12): box('body', 'split_hip_tasset', [x, y, -2.9], [2.9, 2.5, .9], 3, angle)
        for x in (-3.5, 2.9): box('body', 'back_power_bus', [x, 1, 2.5], [.6, 8.5, .6], 6)
        box('body', 'back_center_plate', [-2.5, 1.5, 2.4], [5, 6.5, .7], 2)

    if profile == 'pioneer':
        for x in (-4.6, 4.2):
            box('head', 'ear_defender', [x, -5.5, -1.7], [.4, 3.4, 3.4], 2)
            box('head', 'ear_defender_band', [x, -4.8, -1.1], [.45, .6, 2.2], 5)
        box('head', 'rear_lamp_battery', [-2.2, -6, 4.2], [4.4, 2.1, .6], 2)
        slats('head', -1.5, -5.6, 4.85, 2, 3)
        box('body', 'bib_pocket_seam', [-2.2, 5, -3.25], [4.4, .22, .2], 3)
        for x in (-3.6, 2.8): box('body', 'harness_clasp', [x, 1.6, -3], [.8, 1.2, .4], 3)
    elif profile == 'vanguard':
        for x in (-4.85, 4.45):
            box('head', 'temple_armor', [x, -6.1, -2], [.4, 4.2, 4.5], 3)
            box('head', 'temple_inset', [x - .05, -5.3, -1.3], [.5, 2.5, 2.8], 2)
        for x in (-3.8, 1.8):
            box('head', 'rear_helmet_vent_mount', [x, -6.5, 4.6], [2, 3.5, .5], 1)
            slats('head', x + .2, -6.1, 5.12, 4, 1.6, 3)
        for x in (-2.6, .6): box('body', 'rear_service_plate', [x, 7.6, 2.5], [2, 2.3, .5], 2)
    elif profile == 'ranger':
        for x in (-3.4, 2.7): box('head', 'rear_helmet_contact', [x, -3, 4.2], [.7, 2.2, .35], 3)
        box('head', 'rear_sensor_bus', [-3.4, -6.8, 4.2], [6.8, .5, .3], 5)
        box('body', 'sternum_readout', [-1.8, 2.4, -3.4], [2.4, .3, .2], 6)
        for y in (4, 5.4): box('body', 'right_rib_plate', [2.2, y, -2.65], [1.9, .7, .4], 3)
        box('body', 'rear_radio_clip', [-3.9, 8.3, 2.4], [2.2, 2, .6], 2)
    else:
        for x in (-4.65, 4.25): box('head', 'temple_frame', [x, -6.8, -1.8], [.4, 4.9, 3.5], 3)
        for y in (-7.5, -5.4, -3.3):
            box('head', 'rear_crown_segment', [-2, y, 4.4], [4, 1.4, .45], 3)
            box('head', 'rear_crown_light', [-.3, y + .2, 4.9], [.6, 1, .2], 6)
        for y in (2.5, 4.8, 7.1): box('body', 'back_reactor_rib', [-2.2, y, 3.1], [4.4, .5, .3], 3)

    for arm, side in [('right_arm', -1), ('left_arm', 1)]:
        center = side
        box(arm, 'sleeve_seal', [center - 2.34, -2.34, -2.34], [4.68, 12.68, 4.68], 1)
        if profile == 'pioneer':
            box(arm, 'work_sleeve', [center - 2.4, -1.8, -2.4], [4.8, 5, 4.8], 2)
            box(arm, 'shoulder_patch', [center - 1.6, -1, -2.65], [3.2, 1, .3], 6)
            box(arm, 'wrist_work_cuff', [center - 2.45, 7.5, -2.45], [4.9, 1.5, 4.9], 3)
            if side < 0:
                box(arm, 'wrist_terminal', [center - 1.6, 5.5, -3], [3.2, 2.2, .8], 1)
                box(arm, 'terminal_display', [center - 1.2, 5.9, -3.2], [2.4, 1.3, .25], 6)
        elif profile == 'vanguard':
            for y, width in ((-3.3, 6.8), (-1, 6.2), (1.3, 5.6)):
                box(arm, 'siege_pauldron', [center - width / 2 + side * .6, y, -3.1], [width, 2, 6.2], 2)
                box(arm, 'pauldron_edge', [center - width / 2 + side * .6, y, -3.35], [width, .45, .4], 3)
            box(arm, 'orange_shouldermark', [center - 1, -2.4, -3.4], [2, .6, .25], 6)
            box(arm, 'heavy_gauntlet', [center - 2.7, 5, -3], [5.4, 4.6, 6], 2)
            for y in (5.4, 7): box(arm, 'gauntlet_plate', [center - 2.4, y, -3.35], [4.8, 1.3, .5], 3)
        elif profile == 'ranger':
            if side < 0:
                box(arm, 'single_shoulder_shield', [center - 2.6, -2.8, -2.8], [5.2, 2.8, 5.6], 3)
                box(arm, 'shoulder_green_inset', [center - 1.8, -2.3, -3], [3.6, 1.6, .25], 5)
            else: box(arm, 'flexible_shoulder_strap', [center - 2.4, -1, -2.4], [4.8, .8, 4.8], 5)
            box(arm, 'slim_forearm_plate', [center - 1.6, 5.3, -2.7], [3.2, 3.7, .5], 2)
            box(arm, 'wrist_sensor', [center - .9, 7, -2.95], [1.8, .8, .3], 6)
        else:
            box(arm, 'raised_wing_pauldron', [center - 3, -3.6, -2.9], [6, 2, 5.8], 4, side * -18)
            box(arm, 'lower_wing_plate', [center - 2.8, -.8, -2.8], [5.6, 1.8, 5.6], 3, side * -12)
            box(arm, 'pauldron_energy', [center - 2.4, -2.5, -3.2], [4.8, .3, .35], 6, side * -18)
            for y in (5, 6.8, 8.6): box(arm, 'segmented_vambrace', [center - 2.45, y, -2.8], [4.9, 1.4, 5.6], 3)
            box(arm, 'wrist_channel', [center - .4, 5.3, -3.05], [.8, 4, .3], 6)

    for leg in ('right_leg', 'left_leg'):
        heavy = profile == 'vanguard'; star = profile == 'starwarden'; light = profile == 'ranger'
        width = 5.2 if heavy else 4.6
        box(leg, 'boot_shell', [-width / 2, 8, -2.4], [width, 4.2, 4.9], 2)
        box(leg, 'toe_guard', [-1.8, 10.4, -3.5 if heavy else -3.1], [3.6, 1.7, 2], 3)
        box(leg, 'traction_sole', [-width / 2 - .1, 11.9, -3.6 if heavy else -3.2], [width + .2, .5, 6.2 if heavy else 5.8], 1)
        if heavy or star:
            box(leg, 'shin_tower', [-1.75, 6.9, -2.95], [3.5, 3.5, .8], 4 if star else 3)
            box(leg, 'shin_channel', [-.4, 7.2, -3.2], [.8, 2.7, .3], 6)
        elif light:
            box(leg, 'rear_ankle_stabilizer', [-1.6, 8.4, 2.5], [3.2, 2.6, .8], 3)
        else:
            for y in (8.5, 9.6): box(leg, 'work_boot_lacing', [-1.4, y, -2.65], [2.8, .35, .3], 5)
        for x in (-1.8, .8): box(leg, 'sole_tread', [x, 11.85, -3.3], [.8, .6, .4], 0)
    return parts
