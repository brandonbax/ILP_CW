package uk.ac.ed.inf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;

import java.text.SimpleDateFormat;

public class NodeTests {
    Node node1 = null;
    Node node2 = null;
    @BeforeEach
    void setUp(){
        node1 = new Node(new LngLat(1, 1));
    }

    @Test
    void nodesEqual(){
        assert node1.equals(node1);
        node2 = new Node(new LngLat(1, 1));
        assert node1.equals(node2);
        assert node1.hashCode() == node2.hashCode();
    }

    @Test
    void nodesNotEqual(){
        assert node1 != null;
        String string = "";
        assert !node1.equals(string);

        node2 = new Node(new LngLat(2, 2));
        assert !node1.equals(node2);
        assert node1.hashCode() != node2.hashCode();
    }
}
