package uk.ac.ed.inf;

import org.junit.Test;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import static org.junit.Assert.assertEquals;

public class LngLatHandlerTests {
    LngLatHandler handler = new LngLatHandler();
    NamedRegion region1 = new NamedRegion("region1", new LngLat[]{new LngLat(0, 2), new LngLat(0, 0), new LngLat(2, 0), new LngLat(2, 2)});
    @Test
    public void testDistanceTo(){
        // Horizontal line
        LngLat startPos = new LngLat(2, 3);
        LngLat endPos = new LngLat(4, 3);

        assertEquals(handler.distanceTo(startPos, endPos), 2, 0.00001);
    }
    @Test
    public void testIsInRegion(){
        LngLat[] points = new LngLat[]{new LngLat(0, 0), new LngLat(0.5, 0.5)};

        for (LngLat point: points){
            assert handler.isInRegion(point, region1);
        }
    }
    @Test
    public void testIsNotInRegion(){
        LngLat[] points = new LngLat[]{new LngLat(-0.5, 0), new LngLat(0.5, 5)};

        for (LngLat point: points){
            assert !handler.isInRegion(point, region1);
        }
    }
}
