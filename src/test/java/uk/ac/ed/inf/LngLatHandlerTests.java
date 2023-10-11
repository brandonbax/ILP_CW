package uk.ac.ed.inf;

import org.junit.Test;
import uk.ac.ed.inf.ilp.data.LngLat;

import static org.junit.Assert.assertEquals;

public class LngLatHandlerTests {
    LngLatHandler handler = new LngLatHandler();
    @Test
    public void testDistanceTo(){
        // Horizontal line
        LngLat startPos = new LngLat(2, 3);
        LngLat endPos = new LngLat(4, 3);

        assertEquals(handler.distanceTo(startPos, endPos), 2, 0.00001);
    }
}
