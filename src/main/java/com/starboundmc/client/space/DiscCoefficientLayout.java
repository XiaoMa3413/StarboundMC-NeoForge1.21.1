package com.starboundmc.client.space;

/** Independent guarded 2D charts for a periodic 3D coefficient field and its mips.
 * Hardware mips must never be generated over this atlas: they mix unrelated charts. */
public record DiscCoefficientLayout(int radial, int azimuth, int height) {
    public static final int BANKS=8;
    public DiscCoefficientLayout {
        if(radial<1 || azimuth<radial || height<1 || Integer.bitCount(radial)!=1
                || Integer.bitCount(azimuth)!=1 || Integer.bitCount(height)!=1)
            throw new IllegalArgumentException("Power-of-two medium chart dimensions required");
    }
    public int levels() {return Integer.numberOfTrailingZeros(radial)+1;}
    public int radial(int level) {return Math.max(1,radial>>level);}
    public int azimuth(int level) {return Math.max(1,azimuth>>level);}
    public int height(int level) {return Math.max(1,height>>level);}
    public int width(int level) {return (radial(level)+2)*height(level);}
    public int rows(int level) {return azimuth(level)+2;}
    public int offset(int level) {
        int rows=0;for(int i=0;i<level;i++)rows+=rows(i);return rows;
    }
    public int stride() {return offset(levels());}
    public int width() {return width(0);}
    public int rows() {return stride()*BANKS;}
    public long gpuBytes() {return (long)width()*rows()*8;}
    public static int bank(int generation) {return Math.floorMod(generation,BANKS);}
    /** Both finite-lived fields use this common window, including two prefetched IDs. */
    public static int newest(double time) {return 2*(int)Math.floor((time+35)/70)+3;}
}
