package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

import static uk.ac.ed.inf.FindPath.NUM_OF_DIRECTIONS;

public class LngLatHandler implements LngLatHandling {
    /**
     * @param startPosition is where the start is
     * @param endPosition is where the end is
     * @return the Euclidean distance between the positions
     */
    // The 2 points are treated as points on a plane rather than points on a sphere (Earth)
    // as a simplification since the distance between points is small enough for this to still be accurate.
    @Override
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        // Uses formula for Euclidean distance
        return Math.sqrt(Math.pow(startPosition.lng() - endPosition.lng(), 2) +
                Math.pow(startPosition.lat() - endPosition.lat(), 2));
    }

    /**
     * @param startPosition is the starting position
     * @param otherPosition is the position to check
     * @return if the positions are close
     */
    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) <= SystemConstants.DRONE_IS_CLOSE_DISTANCE;
    }

    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        // Sets initial values for finding max and min
        double minX = region.vertices()[0].lng();
        double maxX = region.vertices()[0].lng();
        double minY = region.vertices()[0].lat();
        double maxY = region.vertices()[0].lat();
        // Finds the min and max x and y values in the given region (polygon)
        for (int i = 1; i < region.vertices().length - 1; i++){
            if (region.vertices()[i].lng() < minX){
                minX = region.vertices()[i].lng();
            }
            if (region.vertices()[i].lng() > maxX){
                maxX = region.vertices()[i].lng();
            }
            if (region.vertices()[i].lat() < minY){
                minY = region.vertices()[i].lat();
            }
            if (region.vertices()[i].lat() > maxY){
                maxY = region.vertices()[i].lat();
            }
        }

        // Using the min and max x and y values, a rectangle can be constructed around the polygon
        // and if the point is outside this rectangle, then it is definitely outside the polygon
        if (position.lng() < minX || position.lng() > maxX || position.lat() < minY || position.lat() > maxY){
            return false;
        }

        // Padding on the start position of the cast ray, to ensure that it starts left of any polygon edge
        double padding = 1E-5;
        // Starts the ray slightly left of the leftmost point in the polygon (so that the first edge is guaranteed to
        // be counted, even with precision errors) at the same y position as the point that is being checked.
        // This essentially gives a horizontal line which stops at the point we are checking
        LngLat rayStartingPoint = new LngLat(minX - padding, position.lat());
        int intersections = 0;
        // Uses linear equations to check all the vertices in the polygon if they intersect with the ray
        for (int i = 0; i < region.vertices().length - 2; i++){
            if (areIntersecting(rayStartingPoint, position, region.vertices()[i], region.vertices()[i+1])){
                intersections++;
            }
        }
        // if odd return true
        return intersections % 2 == 1;
    }

    private boolean areIntersecting(LngLat rayStartingPoint, LngLat point, LngLat vertex1, LngLat vertex2){
        // Simplified some equations since a1 would always be 0 (as the ray is horizontal)
        double b1 = rayStartingPoint.lng() - point.lng();
        double c1 = (point.lng() * rayStartingPoint.lat()) - (rayStartingPoint.lng() * point.lat());

        double d1 = (b1 * vertex1.lat()) + c1;
        double d2 = (b1 * vertex2.lat()) + c1;
        if ((d1 > 0 && d2 > 0) || (d1 < 0 && d2 < 0)){
            return false;
        }

        double a2 = vertex2.lat() - vertex1.lat();
        double b2 = vertex1.lng() - vertex2.lng();
        double c2 = (vertex2.lng() * vertex1.lat()) - (vertex1.lng() * vertex2.lat());

        d1 = (a2 * rayStartingPoint.lng()) + (b2 * rayStartingPoint.lat()) + c2;
        d2 = (a2 * point.lng()) + (b2 * point.lat()) + c2;

        // if the signs of d1 and d2 are the same, then the lines do not intersect
        if ((d1 > 0 && d2 > 0) || (d1 < 0 && d2 < 0)){
            return false;
        }

        // After testing, it seems that if the lines are collinear then that means the bottom edge of the polygon
        // is horizontal and the point has the same y value as that edge. In this case if the case of collinear were
        // to be counted as an intersection then 2 intersections would be counted when it hits the corner where
        // the bottom edge meets the other 2 edges. This would result in a point on the bottom edge of the polygon to
        // be counted as not in the region (when it should be), so any lines that are collinear (if a2 * b1 == 0) are
        // counted as not intersecting.
        return a2 * b1 != 0;
    }

    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        if (angle % ((2 * Math.PI) / NUM_OF_DIRECTIONS) != 0){
            throw new RuntimeException("Invalid angle. Enter an angle on one of the 16 compass points");
        }
        // The next position can be found by breaking the vector into its component forms (longitude and latitude)
        // and adding them to the start position.
        double x = SystemConstants.DRONE_MOVE_DISTANCE * Math.cos(angle);
        double y = SystemConstants.DRONE_MOVE_DISTANCE * Math.sin(angle);
        return new LngLat(startPosition.lng() + x, startPosition.lat() + y);
    }
}
