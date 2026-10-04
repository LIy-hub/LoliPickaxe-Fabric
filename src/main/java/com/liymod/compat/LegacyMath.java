package com.liymod.compat;

public final class LegacyMath {
    private LegacyMath() { }
    public static int clamp(int v,int min,int max) { return Math.max(min,Math.min(max,v)); }
    public static int clamp(long v,int min,int max) { return (int) Math.max(min,Math.min(max,v)); }
    public static double clamp(double v,double min,double max) { return Math.max(min,Math.min(max,v)); }
    public static float clamp(float v,float min,float max) { return Math.max(min,Math.min(max,v)); }
}
