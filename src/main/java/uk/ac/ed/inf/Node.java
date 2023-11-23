package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

import java.util.Objects;

public class Node {

    // The nodes position and parent will not change from when it is initialised, so these variables are final.
    // The A* value parameters are not set at initialisation so must be mutable.
    public final LngLat pos;
    public double f, g;    // A* algorithm function values.
    public final Node parent;
    public final double directionFromParent;

    public Node(Node parent, double directionFromParent, LngLat pos) {
        this.pos = pos;
        this.parent = parent;
        this.directionFromParent = directionFromParent;
        f = 0;
        g = 0;
    }

    @Override
    public int hashCode(){
        return Objects.hash(pos);
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj){
            return true;
        }

        if(obj == null || getClass() != obj.getClass()){
            return false;
        }

        Node other = (Node)obj;
        return other.pos.equals(pos);
    }
}
