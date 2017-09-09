import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

/**
 * User: wangchongyang on 2017/8/31 0031.
 */
class RootEnum {
    public enum MyLabels implements Label {
        JAR
    }

    enum MyRelationshipTypes implements RelationshipType {
        DEPEND_ON
    }
}
