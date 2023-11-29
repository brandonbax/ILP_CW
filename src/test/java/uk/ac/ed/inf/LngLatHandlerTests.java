package uk.ac.ed.inf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

public class LngLatHandlerTests {
    LngLatHandler handler = new LngLatHandler();
    NamedRegion region1 = new NamedRegion("region1", new LngLat[]{new LngLat(0, 2), new LngLat(0, 0), new LngLat(2, 0), new LngLat(2, 2)});
    @Test
    void testDistanceTo(){
        // Horizontal line
        LngLat startPos = new LngLat(2, 3);
        LngLat endPos = new LngLat(4, 3);

        Assertions.assertEquals(handler.distanceTo(startPos, endPos), 2, 0.00001);
    }
    @Test
    void testIsInRegion(){
        LngLat[] points = new LngLat[]{new LngLat(0, 0), new LngLat(0.5, 0.5)};

        for (LngLat point: points){
            assert handler.isInRegion(point, region1);
        }
    }
    @Test
    void testIsNotInRegion(){
        LngLat[] points = new LngLat[]{new LngLat(-0.5, 0), new LngLat(0.5, 5)};

        for (LngLat point: points){
            assert !handler.isInRegion(point, region1);
        }
    }
    @Test
    void invalidAngle(){
        try {
            handler.nextPosition(new LngLat(1, 1), 2);
            Assertions.fail("An exception should have been thrown");
        } catch (RuntimeException e){
            Assertions.assertEquals(RuntimeException.class, e.getClass());
            Assertions.assertEquals("Invalid angle. Enter an angle on one of the 16 compass points", e.getMessage());
        }

    }
}
