package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class CarouselGeometryTest {
    @Test public void firstAndLastCellsHaveRoomToCenter() {
        assertEquals(400, CarouselGeometry.edgeSpace(1000, 200));
        assertEquals(0, CarouselGeometry.edgeSpace(100, 200));
        assertEquals(0, CarouselGeometry.delta(400, 200, 0, 1000));
    }
    @Test public void physicalCoordinatesHandleEitherDirectionAndPadding() {
        assertEquals(-200, CarouselGeometry.delta(250, 100, 50, 950));
        assertEquals(200, CarouselGeometry.delta(650, 100, 50, 950));
        assertEquals(0, CarouselGeometry.delta(0, 0, 50, 950));
        assertEquals(0, CarouselGeometry.delta(0, 100, 50, 50));
    }
}
