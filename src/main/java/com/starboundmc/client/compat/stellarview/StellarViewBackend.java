package com.starboundmc.client.compat.stellarview;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import com.mojang.datafixers.util.Either;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.StarfieldClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Public-API bridge, loaded only after the optional mod boundary confirms presence. */
final class StellarViewBackend {
    private static final long FIELD_SEED=0x5B4D434CL;
    private static Api api;
    private static Object stars;
    private static final StellarViewPositionAdapter position=new StellarViewPositionAdapter();
    private static int starBudget;
    private StellarViewBackend() {}

    static boolean render(ClientLevel level,Camera camera,float partialTick,Matrix4f modelView,Matrix4f projection,
                          SpaceRenderContext space,float brightness,float convergence,Vector3f forward,boolean linear) {
        try {
            Api binding=api();
            int budget=StarfieldClientConfig.SPACE_VISUAL_QUALITY.get().backgroundStarBudget();
            if (stars != null && budget != starBudget) reset();
            position.setPosition(space.universePosition());
            if (stars == null) {
                Object coordinates=Proxy.newProxyInstance(binding.coordinatesInterface.getClassLoader(),
                        new Class<?>[] {binding.coordinatesInterface},(proxy,method,args) -> {
                            if (method.getName().equals("sample")) return binding.coordinates(position.sample());
                            if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                case "toString" -> "StarboundMC virtual ship coordinates";
                                default -> throw new UnsupportedOperationException(method.toString());
                            };
                            throw new UnsupportedOperationException(method.toString());
                        });
                Object field=binding.field.newInstance(Optional.empty(),Either.left(binding.coordinates.newInstance()),
                        binding.axisNone,0,Optional.empty(),binding.dustTexture,false,binding.stretch,
                        budget,Optional.of(ResourceLocation.fromNamespaceAndPath("starboundmc","cinematic_stars")),
                        binding.starTexture,false,binding.stretch,FIELD_SEED,1_000_000,List.of());
                stars=binding.external.newInstance(field,coordinates);
                starBudget=budget;
            }
            if (linear) return binding.linear != null && (boolean)binding.linear.invoke(stars,level,camera,partialTick,
                    modelView,projection,brightness,convergence,forward,.7F);
            return (boolean)binding.render.invoke(stars,level,camera,partialTick,modelView,projection,brightness,convergence,forward);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Stellar View API bridge failed",failure); }
    }

    static boolean supportsLinear() {
        try { return api().linear != null; }
        catch (ReflectiveOperationException failure) { return false; }
    }

    static void reset() {
        try { if (stars != null) api.reset.invoke(stars); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Stellar View reset failed",failure); }
        finally { stars=null; }
    }

    private static Api api() throws ReflectiveOperationException {
        if (api == null) api=new Api();
        return api;
    }

    /** Resolve public signatures once; no deep access or method discovery in the draw loop. */
    private static final class Api {
        final Class<?> coordinatesInterface=Class.forName("net.povstalec.stellarview.api.client.ExternalViewCenterCoords");
        final Class<?> coords=Class.forName("net.povstalec.stellarview.common.util.SpaceCoords");
        final Class<?> axis=Class.forName("net.povstalec.stellarview.common.util.AxisRotation");
        final Class<?> starField=Class.forName("net.povstalec.stellarview.api.common.space_objects.resourcepack.StarField");
        final Class<?> stretchType=Class.forName("net.povstalec.stellarview.api.common.space_objects.resourcepack.StarField$Stretch");
        final Class<?> externalType=Class.forName("net.povstalec.stellarview.api.client.ExternalStarField");
        final Constructor<?> coordinates=coords.getConstructor();
        final Constructor<?> positioned=coords.getConstructor(long.class,long.class,long.class,double.class,double.class,double.class);
        final Constructor<?> field=starField.getConstructor(Optional.class,Either.class,axis,int.class,Optional.class,
                ResourceLocation.class,boolean.class,stretchType,int.class,Optional.class,ResourceLocation.class,
                boolean.class,stretchType,long.class,int.class,List.class);
        final Constructor<?> external=externalType.getConstructor(starField,coordinatesInterface);
        final Object axisNone=axis.getField("NONE").get(null),stretch=stretchType.getField("DEFAULT_STRETCH").get(null);
        final Object dustTexture=starField.getField("DEFAULT_DUST_CLOUD_TEXTURE").get(null),
                starTexture=starField.getField("DEFAULT_STAR_TEXTURE").get(null);
        final Method render=externalType.getMethod("render",ClientLevel.class,Camera.class,float.class,Matrix4f.class,
                Matrix4f.class,float.class,float.class,Vector3f.class);
        final Method reset=externalType.getMethod("reset"),linear;
        Api() throws ReflectiveOperationException {
            Method optional;
            try { optional=externalType.getMethod("renderLinear",ClientLevel.class,Camera.class,float.class,Matrix4f.class,
                    Matrix4f.class,float.class,float.class,Vector3f.class,float.class); }
            catch (NoSuchMethodException missing) { optional=null; }
            linear=optional;
        }
        Object coordinates(StellarViewPositionAdapter.Coordinates c) throws ReflectiveOperationException {
            return positioned.newInstance(c.xLy(),c.yLy(),c.zLy(),c.xKm(),c.yKm(),c.zKm());
        }
    }
}
