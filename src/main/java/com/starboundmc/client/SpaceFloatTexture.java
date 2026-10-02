package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

/** Tightly packed CPU float transfers must not inherit NativeImage atlas or foreign PBO state. */
final class SpaceFloatTexture {
    private SpaceFloatTexture() {}
    static int upload(int width,int height,int channels,float[] values) {
        RenderSystem.assertOnRenderThread();
        if(width<=0 || height<=0 || values.length!=(long)width*height*channels)
            throw new IllegalArgumentException("Invalid float texture extent");
        int format=switch(channels){case 1->GL11.GL_RED;case 2->GL30.GL_RG;case 4->GL11.GL_RGBA;default->throw new IllegalArgumentException("Unsupported channels");};
        int internal=switch(channels){case 1->GL30.GL_R32F;case 2->GL30.GL_RG32F;default->GL30.GL_RGBA32F;};
        int[] queries={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_PIXELS,
                GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SWAP_BYTES};
        int[] previous=new int[queries.length];for(int i=0;i<queries.length;i++)previous[i]=GL11.glGetInteger(queries[i]);
        int buffer=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING),binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        var data=MemoryUtil.memAllocFloat(values.length);int texture=0;boolean complete=false;
        try {
            data.put(values).flip();texture=GlStateManager._genTexture();GlStateManager._bindTexture(texture);
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);
            for(int i=0;i<queries.length;i++)GlStateManager._pixelStore(queries[i],i==0?4:0);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,internal,width,height,0,format,GL11.GL_FLOAT,data);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            if(GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH)!=width
                    || GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT)!=height)
                throw new IllegalStateException("Float texture allocation failed");
            complete=true;return texture;
        } finally {
            if(!complete && texture!=0)GlStateManager._deleteTexture(texture);
            MemoryUtil.memFree(data);
            for(int i=0;i<queries.length;i++)GlStateManager._pixelStore(queries[i],previous[i]);
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,buffer);GlStateManager._bindTexture(binding);
        }
    }
}
