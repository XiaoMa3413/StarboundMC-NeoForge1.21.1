package com.starboundmc.armor;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ArmorItem;

/** One tuning table for materials, individual equipment bonuses and four-piece bonuses. */
public enum ArmorProfile {
    PIONEER(2, 5, 4, 1, 18, 0, 12),
    VANGUARD(3, 8, 6, 3, 32, 2, 14),
    RANGER(2, 6, 5, 3, 26, 1, 18),
    STARWARDEN(3, 9, 7, 3, 40, 3, 20);

    public final int helmet, chestplate, leggings, boots, durability, enchantability;
    public final float toughness;
    ArmorProfile(int h, int c, int l, int b, int durability, float toughness, int enchantability) {
        this.helmet = h; this.chestplate = c; this.leggings = l; this.boots = b;
        this.durability = durability; this.toughness = toughness; this.enchantability = enchantability;
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public record Bonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {}
    private static Bonus add(Holder<Attribute> attribute, double amount) {
        return new Bonus(attribute, amount, AttributeModifier.Operation.ADD_VALUE);
    }
    private static Bonus percent(Holder<Attribute> attribute, double amount) {
        return new Bonus(attribute, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
    public List<Bonus> piece(ArmorItem.Type type) {
        return switch (this) {
            case PIONEER -> switch (type) {
                case HELMET -> List.of(percent(Attributes.BLOCK_BREAK_SPEED, .15));
                case CHESTPLATE -> List.of(add(Attributes.MAX_HEALTH, 2));
                case LEGGINGS -> List.of(percent(Attributes.MOVEMENT_SPEED, .04));
                case BOOTS -> List.of(add(Attributes.SAFE_FALL_DISTANCE, 2));
                default -> List.of();
            };
            case VANGUARD -> switch (type) {
                case HELMET -> List.of(add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, .15));
                case CHESTPLATE -> List.of(add(Attributes.MAX_HEALTH, 4));
                case LEGGINGS -> List.of(add(Attributes.ATTACK_DAMAGE, 1));
                case BOOTS -> List.of(add(Attributes.KNOCKBACK_RESISTANCE, .10));
                default -> List.of();
            };
            case RANGER -> switch (type) {
                case HELMET -> List.of(); // +5% projectile damage is evaluated at the hit, server-side.
                case CHESTPLATE -> List.of(add(Attributes.MAX_HEALTH, 2));
                case LEGGINGS -> List.of(percent(Attributes.MOVEMENT_SPEED, .08));
                case BOOTS -> List.of(add(Attributes.SAFE_FALL_DISTANCE, 3));
                default -> List.of();
            };
            case STARWARDEN -> switch (type) {
                case HELMET -> List.of(percent(Attributes.BLOCK_BREAK_SPEED, .25));
                case CHESTPLATE -> List.of(add(Attributes.MAX_HEALTH, 6));
                case LEGGINGS -> List.of(add(Attributes.ATTACK_DAMAGE, 2));
                case BOOTS -> List.of(add(Attributes.SAFE_FALL_DISTANCE, 4));
                default -> List.of();
            };
        };
    }
    public List<Bonus> fullSet() {
        return switch (this) {
            case PIONEER -> List.of(percent(Attributes.BLOCK_BREAK_SPEED, .15), add(Attributes.MAX_HEALTH, 2));
            case VANGUARD -> List.of(add(Attributes.ATTACK_DAMAGE, 2), add(Attributes.KNOCKBACK_RESISTANCE, .10));
            case RANGER -> List.of(percent(Attributes.MOVEMENT_SPEED, .04));
            case STARWARDEN -> List.of(add(Attributes.MAX_HEALTH, 4),
                    new Bonus(Attributes.ATTACK_SPEED, .10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        };
    }
    public double projectileBonus(boolean helmetEquipped, boolean fullSet) {
        return this == RANGER ? (helmetEquipped ? .05 : 0) + (fullSet ? .10 : 0)
                : this == STARWARDEN && fullSet ? .15 : 0;
    }
}
