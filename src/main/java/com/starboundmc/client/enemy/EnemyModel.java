package com.starboundmc.client.enemy;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.starboundmc.enemy.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Bone-local triangles extracted from the editable, reopened Blockbench projects. */
public final class EnemyModel extends HierarchicalModel<MachineEnemy> {
    private final ModelPart root;
    private final Map<String,ModelPart> bones=new HashMap<>();
    public EnemyModel(EnemyProfile profile) {
        var location=ResourceLocation.fromNamespaceAndPath("starboundmc","enemies/"+profile.id+".json");
        JsonArray source;
        try (var reader=Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            source=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("bones");
        } catch (IOException e) { throw new IllegalStateException("Cannot load enemy model "+profile.id,e); }
        Map<String,JsonObject> definitions=new LinkedHashMap<>();
        for(var node:source) {
            var bone=node.getAsJsonObject();var name=bone.get("name").getAsString();
            if(definitions.put(name,bone)!=null)throw new IllegalArgumentException("Duplicate enemy bone: "+name);
        }
        root=bake("root",definitions,new HashSet<>());
        if(bones.size()!=definitions.size())throw new IllegalArgumentException("Unreachable enemy bone");
    }
    private ModelPart bake(String name,Map<String,JsonObject> definitions,Set<String> active) {
        if(!active.add(name))throw new IllegalArgumentException("Cyclic enemy bone: "+name);
        var definition=Objects.requireNonNull(definitions.get(name),"Missing enemy bone "+name);
        Map<String,ModelPart> children=new HashMap<>();
        for(var entry:definitions.entrySet()) {
            var parent=entry.getValue().get("parent");
            if(!parent.isJsonNull()&&parent.getAsString().equals(name))children.put(entry.getKey(),bake(entry.getKey(),definitions,active));
        }
        List<ModelPart.Cube> cubes=new ArrayList<>();
        for(var part:definition.getAsJsonArray("parts"))cubes.add(new MeshCube(part.getAsJsonObject()));
        var model=new ModelPart(cubes,children);var offset=definition.getAsJsonArray("offset");
        var pose=PartPose.offset(offset.get(0).getAsFloat(),offset.get(1).getAsFloat(),offset.get(2).getAsFloat());
        model.setInitialPose(pose);model.loadPose(pose);bones.put(name,model);active.remove(name);return model;
    }
    @Override public ModelPart root(){return root;}
    private ModelPart bone(String name){return Objects.requireNonNull(bones.get(name),"Missing animated bone "+name);}
    private static float smooth(float t){return t*t*(3-2*t);}
    @Override public void setupAnim(MachineEnemy mob,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float progress=mob.animationProgress(age-mob.tickCount);
        float lift=mob.phase()==MachineEnemy.WINDUP?smooth(progress):mob.phase()==MachineEnemy.ACTIVE?1-smooth(progress):0;
        float walk=Math.min(limbAmount,1);var body=bone("body");
        switch(mob.profile()) {
            case SCRAP_SKITTER -> {
                body.y+=Mth.sin(limbSwing*1.5f)*walk*.35f;
                for(String prefix:List.of("front","rear"))for(String side:List.of("right","left")) {
                    String name=prefix+"_"+side;float phase=(prefix.equals("front")==side.equals("right"))?0:Mth.PI;
                    float stride=Mth.sin(limbSwing*1.6f+phase)*walk;
                    bone(name).xRot=stride*.45f-(prefix.equals("front")?lift*.65f:0);
                    bone(name).zRot=stride*.18f*(side.equals("right")?-1:1);
                    bone(name+"_lower").xRot=-stride*.25f;
                }
                bone("sensor").yRot=Mth.sin(age*.035f)*.08f;
                bone("mandible_right").yRot=lift*.25f;bone("mandible_left").yRot=-lift*.25f;
            }
            case COIL_SENTRY -> {
                body.y-=Mth.sin(age*.075f)*.65f;
                body.zRot=Mth.sin(age*.035f)*.025f;
                bone("eye").xRot=Mth.clamp(pitch*Mth.DEG_TO_RAD,-.25f,.25f);
                bone("cannon").yRot=Mth.clamp(yaw*Mth.DEG_TO_RAD,-.3f,.3f);
                bone("coil_right").zRot=-lift*.1f;bone("coil_left").zRot=lift*.1f;
                bone("cannon").z+=lift*.9f;
                bone("cannon").z+=mob.phase()==MachineEnemy.ACTIVE?Mth.sin(progress*Mth.PI)*.6f:0;
                bone("rear_rotor").zRot=age*.14f;
                bone("fin_right").xRot=Mth.sin(age*.05f)*.025f;bone("fin_left").xRot=-Mth.sin(age*.05f)*.025f;
            }
            case BREACH_GUARD -> {
                bone("head").yRot=Mth.clamp(yaw*Mth.DEG_TO_RAD,-.6f,.6f);bone("head").xRot=Mth.clamp(pitch*Mth.DEG_TO_RAD,-.3f,.3f);
                for(String side:List.of("right","left")) {
                    float stride=Mth.cos(limbSwing*.85f+(side.equals("left")?Mth.PI:0))*walk;
                    bone(side+"_leg").xRot=stride*.45f;bone(side+"_leg_lower").xRot=Math.max(0,-stride)*.3f;
                }
                bone("right_arm").xRot=-lift*1.3f;bone("left_arm").xRot=mob.shieldRaised()?-.3f:0;
                bone("drill").yRot=age*(mob.phase()==MachineEnemy.WINDUP?.75f:.15f);
            }
            case QUARRY_WARDEN -> {
                body.y+=Mth.cos(limbSwing*.8f)*walk*.25f;
                for(String prefix:List.of("front","rear"))for(String side:List.of("right","left")) {
                    String name=prefix+"_"+side;float phase=(prefix.equals("front")==side.equals("right"))?0:Mth.PI;
                    float stride=Mth.cos(limbSwing*.8f+phase)*walk;
                    bone(name).xRot=stride*.3f;bone(name+"_lower").xRot=-stride*.2f;
                }
                bone("core").y+=Mth.sin(age*.07f)*.08f;
                if(mob.action()==MachineEnemy.STOMP)bone("hammer_arm").xRot=-lift*1.4f;
                else {
                    bone("clamp_arm").xRot=-lift*.65f;
                    bone("clamp_arm").yRot=mob.phase()==MachineEnemy.ACTIVE?Mth.sin(progress*Mth.PI)*.9f:lift*.25f;
                }
                bone("clamp_inner").zRot=-lift*.3f;bone("clamp_outer").zRot=lift*.3f;
            }
        }
    }
    private static final class MeshCube extends ModelPart.Cube {
        private record Face(float[][] vertices,Vector3f normal){}
        private final List<Face> faces=new ArrayList<>();
        private final boolean emissive;
        MeshCube(JsonObject part) {
            super(0,0,0,0,0,0,0,0,0,0,0,false,1,1,Set.of());emissive=part.get("emissive").getAsBoolean();
            for(var value:part.getAsJsonArray("triangles")) {
                var triangle=value.getAsJsonArray();if(triangle.size()!=3)throw new IllegalArgumentException("Expected enemy triangle");
                float[][] vertices=new float[3][5];
                for(int i=0;i<3;i++) {
                    var vertex=triangle.get(i).getAsJsonArray();if(vertex.size()!=5)throw new IllegalArgumentException("Enemy vertex requires XYZUV");
                    for(int j=0;j<5;j++){float v=vertex.get(j).getAsFloat();if(!Float.isFinite(v))throw new IllegalArgumentException("Non-finite enemy vertex");vertices[i][j]=j<3?v/16:v;}
                }
                var normal=position(vertices[1]).sub(position(vertices[0])).cross(position(vertices[2]).sub(position(vertices[0])));
                if(normal.lengthSquared()>1e-15f)faces.add(new Face(vertices,normal.normalize()));
            }
        }
        private static Vector3f position(float[] v){return new Vector3f(v[0],v[1],v[2]);}
        @Override public void compile(PoseStack.Pose pose,VertexConsumer buffer,int light,int overlay,int color) {
            var normal=new Vector3f();var position=new Vector3f();
            for(var face:faces){pose.transformNormal(face.normal(),normal);
                for(int i=0;i<4;i++){float[] v=face.vertices()[Math.min(i,2)];pose.pose().transformPosition(v[0],v[1],v[2],position);
                    buffer.addVertex(position.x,position.y,position.z,color,v[3],v[4],overlay,emissive?15728880:light,normal.x,normal.y,normal.z);}
            }
        }
    }
}
