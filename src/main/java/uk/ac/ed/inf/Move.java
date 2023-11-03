package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

public record Move(LngLat startPos, LngLat endPos, double direction) {
}
