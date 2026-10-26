package com.gathin.tvhome;

/** Center focus without wrapping or replacing the native app model. Physical coordinates also work in RTL. */
final class CarouselGeometry {
    static int edgeSpace(int viewport, int cell) { return Math.max(0, (viewport - cell) / 2); }
    static int delta(int left, int width, int visibleLeft, int visibleRight) {
        return visibleRight <= visibleLeft || width <= 0 ? 0 : left + width / 2 - (visibleLeft + visibleRight) / 2;
    }
    private CarouselGeometry() { }
}
