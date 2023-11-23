package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.*;

public class FindPath {

    public static final int NUM_OF_DIRECTIONS = 16;
    private static final LngLatHandler lngLatHandler = new LngLatHandler();

    public static ArrayList<Node> findShortestPath(LngLat start, LngLat goal, NamedRegion[] noFlyZones){
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(c -> c.f));     // frontier
        HashSet<Node> closedSet = new HashSet<>();         // visited
        Node startNode = new Node(null, 999, start);
        startNode.f = lngLatHandler.distanceTo(start, goal);
        openSet.add(startNode);

        while (!openSet.isEmpty()){
            Node current = openSet.poll();
            closedSet.add(current);

            if (lngLatHandler.isCloseTo(current.pos, goal)){
                // Stores the reverse of the path found by A*
                ArrayList<Node> reversePath = new ArrayList<>();
                // Adds a move to represent the drone hovering when it arrives at the restaurant
                reversePath.add(new Node(current.parent, 999, current.pos));

                // Traces the path back the way from the goal to the start, i.e. from the restaurant to Appleton tower
                while (current != null){
                    reversePath.add(current);
                    current = current.parent;
                }
                ArrayList<Node> path = new ArrayList<>(reversePath);
                Collections.reverse(path);
                // The hover move is already in path, so remove it from reversePath
                reversePath.remove(0);
                // Path has all the moves from Appleton tower to the restaurant, so the path back from the restaurant
                // to Appleton tower must be added.
                path.addAll(reversePath);
                return path;
            }

            // Searches the neighbours of the current node
            for (int i = 0; i < NUM_OF_DIRECTIONS; i++){
                // The full rotation angle (2*pi) is split by the number of directions.
                // This will give NUM_OF_DIRECTIONS points of equal distance from each other and from the centre point.
                double angle = ((2 * Math.PI) / NUM_OF_DIRECTIONS) * i;
                LngLat neighbourPosition = lngLatHandler.nextPosition(current.pos, angle);
                Node neighbourNode = new Node(current, angle, neighbourPosition);

                // If the new node is close to a visited node or is in a no-fly zone, then do not consider it
                if (closedSet.contains(neighbourNode) || isInAnyNoFlyZone(neighbourNode.pos, noFlyZones)){
                    continue;
                }

                // Calculates the cost function for this node and adds it to the openSet
                neighbourNode.f = current.g + lngLatHandler.distanceTo(neighbourNode.pos, goal);
                openSet.add(neighbourNode);
            }
        }
        return null;
    }

    // Even though the method must check if the point is in every no-fly zone, it should still be efficient
    // as points that are outside the rectangular bounding box around the noFlyZone polygon will quickly return false.
    private static boolean isInAnyNoFlyZone(LngLat position, NamedRegion[] noFlyZones){
        LngLatHandler lngLatHandler = new LngLatHandler();
        for (NamedRegion noFlyZone: noFlyZones){
            if (lngLatHandler.isInRegion(position, noFlyZone)){
                return true;
            }
        }
        return false;
    }
}
