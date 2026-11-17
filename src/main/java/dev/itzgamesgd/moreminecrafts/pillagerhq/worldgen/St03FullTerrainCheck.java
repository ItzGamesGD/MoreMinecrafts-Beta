package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.function.IntBinaryOperator;

/** Owner decision 2026-11-17: reject an otherwise eligible site when an unsampled column exceeds the limits. */
public final class St03FullTerrainCheck {
    private St03FullTerrainCheck() {}
    public enum Verdict { ELIGIBLE, CUT, SKIRT, FOOTING_FILL }
    public record Result(Verdict verdict,int cut,int skirt,int footingFill,int columnsChecked) {
        public boolean eligible(){return verdict==Verdict.ELIGIBLE;}
    }
    /** Keep the 225-sample foundation unchanged; this check never relocates it or rejects underground cavities. */
    public static Result evaluate(IntBinaryOperator surface,IntBinaryOperator floor,int x0,int z0,int foundationY) {
        int cut=0,skirt=0,fill=0,n=0;
        for(int x=x0;x<x0+112;x++)for(int z=z0;z<z0+112;z++) {
            int s=surface.applyAsInt(x,z),f=floor.applyAsInt(x,z);n++;
            cut=Math.max(cut,s-foundationY);skirt=Math.max(skirt,foundationY-s);fill=Math.max(fill,foundationY-f);
            if(cut>St03PlacementConfig.MAX_CUT)return new Result(Verdict.CUT,cut,skirt,fill,n);
            if(skirt>St03PlacementConfig.MAX_SKIRT)return new Result(Verdict.SKIRT,cut,skirt,fill,n);
            if(fill>St03PlacementConfig.MAX_FOOTING_FILL)return new Result(Verdict.FOOTING_FILL,cut,skirt,fill,n);
        }
        return new Result(Verdict.ELIGIBLE,cut,skirt,fill,n);
    }
}
