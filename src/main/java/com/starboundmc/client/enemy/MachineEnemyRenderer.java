package com.starboundmc.client.enemy;

import com.starboundmc.enemy.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class MachineEnemyRenderer extends MobRenderer<MachineEnemy,EnemyModel> {
    private final ResourceLocation texture;
    public MachineEnemyRenderer(EntityRendererProvider.Context context,EnemyProfile profile) {
        super(context,new EnemyModel(profile),profile.width*.45f);
        texture=ResourceLocation.fromNamespaceAndPath("starboundmc","textures/entity/enemies/"+profile.id+".png");
    }
    @Override public ResourceLocation getTextureLocation(MachineEnemy mob){return texture;}
}
