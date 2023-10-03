package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

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
        double minX = region.vertices()[0].lng();
        double maxX = region.vertices()[0].lng();
        double minY = region.vertices()[0].lat();
        double maxY = region.vertices()[0].lat();
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

        if (position.lng() < minX || position.lng() > maxX || position.lat() < minY || position.lat() > maxY){
            return false;
        }

        // Padding on the start position of the cast ray, to ensure that it starts left of any polygon edge
        double padding = 1E-5;
        LngLat rayStartingPoint = new LngLat(minX - padding, position.lat());
        int intersections = 0;
        for (int i = 0; i < region.vertices().length - 2; i++){
            if (areIntersecting(rayStartingPoint, position, region.vertices()[i], region.vertices()[i+1])){
                intersections++;
            }
        }
        // if odd return true
        return intersections % 2 == 1;
    }

    private boolean areIntersecting(LngLat rayStartingPoint, LngLat point, LngLat vertex1, LngLat vertex2){
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
        if ((d1 > 0 && d2 > 0) || (d1 < 0 && d2 < 0)){
            return false;
        }

        return a2 * b1 != 0;
    }

    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        if (angle % 22.5 != 0){
            throw new RuntimeException("Invalid angle. Enter an angle on one of the 16 compass points");
        }
        // The next position can be found by breaking the vector into its component forms (longitude and latitude)
        // and adding them to the start position.
        double x = SystemConstants.DRONE_MOVE_DISTANCE * Math.cos(angle);
        double y = SystemConstants.DRONE_MOVE_DISTANCE * Math.sin(angle);
        return new LngLat(startPosition.lng() + x, startPosition.lat() + y);
    }
}
