package com.starboundmc.client.space;

/** Relative photometric flux and a small spectral palette; no per-frame spectral integration. */
public final class StarPhotometry {
    private static final int PALETTE_SIZE = 256;
    private static final double MIN_KELVIN = 3500, MAX_KELVIN = 15000;
    private static final Color[] PALETTE = new Color[PALETTE_SIZE];
    static {
        for (int i=0;i<PALETTE_SIZE;i++)
            PALETTE[i] = integrate(MIN_KELVIN + (MAX_KELVIN-MIN_KELVIN)*i/(PALETTE_SIZE-1));
    }
    private StarPhotometry() {}

    public record Color(float red, float green, float blue) {}

    public static float flux(double magnitude) {
        if (!Double.isFinite(magnitude)) throw new IllegalArgumentException("Magnitude must be finite");
        return (float) (8 * Math.pow(10, -.4 * (magnitude-1.2)));
    }

    public static Color color(double kelvin) {
        if (!Double.isFinite(kelvin) || kelvin < MIN_KELVIN || kelvin > MAX_KELVIN)
            throw new IllegalArgumentException("Star colour temperature must be in [3500,15000] K");
        double p = (kelvin-MIN_KELVIN)/(MAX_KELVIN-MIN_KELVIN)*(PALETTE_SIZE-1);
        int i = Math.min((int)p,PALETTE_SIZE-2);
        float t = (float)(p-i);
        Color a=PALETTE[i],b=PALETTE[i+1];
        return new Color(a.red+(b.red-a.red)*t,a.green+(b.green-a.green)*t,a.blue+(b.blue-a.blue)*t);
    }

    public static float display(float linear) {
        return linear <= .0031308F ? linear*12.92F : 1.055F*(float)Math.pow(linear,1/2.4)-.055F;
    }

    private static Color integrate(double kelvin) {
        double x=0,y=0,z=0;
        // Planck spectrum, 5 nm bins; Wyman/Sloan/Shirley 2013 CIE 1931 fits, Eq. 4.
        // https://jcgt.org/published/0002/02/01/
        for (double wavelength=380;wavelength<=780;wavelength+=5) {
            double energy = Math.pow(560/wavelength,5)/Math.expm1(1.438776877e7/(wavelength*kelvin));
            x += energy*(.362*gaussian(wavelength,442,.0624,.0374)
                    +1.056*gaussian(wavelength,599.8,.0264,.0323)-.065*gaussian(wavelength,501.1,.049,.0382));
            y += energy*(.821*gaussian(wavelength,568.8,.0213,.0247)
                    +.286*gaussian(wavelength,530.9,.0613,.0322));
            z += energy*(1.217*gaussian(wavelength,437,.0845,.0278)
                    +.681*gaussian(wavelength,459,.0385,.0725));
        }
        double r=Math.max(0,3.2406*x-1.5372*y-.4986*z),
                g=Math.max(0,-.9689*x+1.8758*y+.0415*z),
                b=Math.max(0,.0557*x-.204*y+1.057*z);
        double maximum=Math.max(r,Math.max(g,b));
        return new Color((float)(r/maximum),(float)(g/maximum),(float)(b/maximum));
    }

    private static double gaussian(double wavelength,double mean,double left,double right) {
        double t=(wavelength-mean)*(wavelength < mean ? left : right);
        return Math.exp(-.5*t*t);
    }
}
