package com.gathin.tvhome;

/** Safe cinematic positions without changing the owner's saved icon-size, spacing or app order. */
final class ShowcaseLayout {
    final float top,brand,headline,cell,gap,width;
    ShowcaseLayout(String position,int count,int spacing){
        if(count<1||count>512||!TileSpacing.valid(spacing))throw new IllegalArgumentException("Invalid dock");
        top="top".equals(position)?178:"middle".equals(position)?288:369;
        brand="top".equals(position)?324:"middle".equals(position)?126:215;
        headline=brand+25;gap=spacing;int visible=Math.min(8,count);cell=(892-(visible-1)*gap)/visible;width=count*cell+(count-1)*gap;
    }
    float center(int index){return index*(cell+gap)+cell/2;}
}
