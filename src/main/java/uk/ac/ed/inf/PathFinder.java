package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.*;

public class PathFinder {

    public static final int NUM_OF_DIRECTIONS = 16;
    private final LngLatHandler lngLatHandler = new LngLatHandler();

    /**
     * @param start is the start of the path
     * @param goal is the goal of the path
     * @param noFlyZones are the no-fly zones
     * @param centralArea is the central area
     * @return the shortest path from {@code start} to {@code goal} or null if there is no path
     */
    public ArrayList<Node> findShortestPath(LngLat start, LngLat goal, NamedRegion[] noFlyZones, NamedRegion centralArea){
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(c -> c.f));     // frontier
        HashSet<Node> closedSet = new HashSet<>();         // visited
        Node startNode = new Node(start);
        // Sets the properties of the startNode
        startNode.h = lngLatHandler.distanceTo(start, goal);
        startNode.f = startNode.h;
        startNode.directionFromParent = 999;
        openSet.add(startNode);
        // Flag to keep path outside the central area once it leaves
        boolean leftCentralArea = false;

        while (!openSet.isEmpty()){
            // To prevent a heap overflow, if a path cannot be calculated within a certain number of loops,
            // then the method should return null. The number could probably be lower, however, I don't want
            // the method to fail when there is a hard to calculate path.
            if (closedSet.size() > 300000){
                return null;
            }
            Node current = openSet.poll();
            closedSet.add(current);

            // Only bothers checking when the flag is false for efficiency.
            if (!leftCentralArea && !lngLatHandler.isInRegion(current.pos, centralArea)){
                // Sets a flag so that when the path leaves the central area it will not reenter it
                // (Prevents the path from going in and out of the central area).
                leftCentralArea = true;
                // Also clears the openSet as all of them would be invalid since they are in the central area.
                openSet.clear();
            }

            if (lngLatHandler.isCloseTo(current.pos, goal)){
                // Stores the reverse of the path found by A*
                ArrayList<Node> reversePath = new ArrayList<>();

                // Traces the path back the way from the goal to the start
                while (current != null){
                    reversePath.add(current);
                    current = current.parent;
                }
                Collections.reverse(reversePath);
                return reversePath;
            }

            // Searches the neighbours of the current node
            for (int i = 0; i < NUM_OF_DIRECTIONS; i++){
                // The full rotation angle (2*pi) is split by the number of directions.
                // This will give NUM_OF_DIRECTIONS points of equal distance from each other and from the centre point.
                double angle = (360.0 / NUM_OF_DIRECTIONS) * i;
                LngLat neighbourPosition = lngLatHandler.nextPosition(current.pos, angle);

                // If searching within the central area, nodes cannot be created outside it. Also, duplicate nodes
                // and nodes within a no-fly zone should not be created
                if (closedSet.contains(new Node(neighbourPosition)) || isInAnyNoFlyZone(neighbourPosition, noFlyZones)
                || (leftCentralArea && lngLatHandler.isInRegion(neighbourPosition, centralArea))){
                    continue;
                }

                double tentativeG = current.g + SystemConstants.DRONE_MOVE_DISTANCE;

                Node existingNode = findNearbyNode(neighbourPosition, openSet);
                if (closedSet.contains(existingNode)){
                    continue;
                }
                if (existingNode != null){
                    // Since the nodes are at the same location, there is no need to check estimated distance;
                    // only the distance travelled so far (g).
                    if (tentativeG < existingNode.g){
                        existingNode.parent = current;
                        existingNode.directionFromParent = angle;
                        existingNode.g = tentativeG;
                        existingNode.h = lngLatHandler.distanceTo(existingNode.pos, goal);
                        existingNode.f = existingNode.g + existingNode.h;
                    }
                } else {
                    Node neighbourNode = new Node(neighbourPosition);
                    neighbourNode.parent = current;
                    neighbourNode.directionFromParent = angle;
                    neighbourNode.g = tentativeG;
                    neighbourNode.h = lngLatHandler.distanceTo(neighbourNode.pos, goal);
                    neighbourNode.f = neighbourNode.g + neighbourNode.h;

                    openSet.add(neighbourNode);
                }
            }
        }
        return null;
    }

    // Even though the method must check if the point is in every no-fly zone, it should still be efficient
    // as points that are outside the rectangular bounding box around the noFlyZone polygon will quickly return false.
    private boolean isInAnyNoFlyZone(LngLat position, NamedRegion[] noFlyZones){
        for (NamedRegion noFlyZone: noFlyZones){
            if (lngLatHandler.isInRegion(position, noFlyZone)){
                return true;
            }
        }
        return false;
    }

    private Node findNearbyNode(LngLat newNodePosition, PriorityQueue<Node> openSet){
        if(openSet.isEmpty()){
            return null;
        }

        Iterator<Node> iterator = openSet.iterator();

        Node find = null;
        while (iterator.hasNext()) {
            Node next = iterator.next();
            if(Math.abs(next.pos.lng() - newNodePosition.lng()) < (SystemConstants.DRONE_IS_CLOSE_DISTANCE * 0.1) &&
                    Math.abs(next.pos.lat() - newNodePosition.lat()) < (SystemConstants.DRONE_IS_CLOSE_DISTANCE * 0.1)){
                find = next;
                break;
            }
        }
        return find;
    }

}
