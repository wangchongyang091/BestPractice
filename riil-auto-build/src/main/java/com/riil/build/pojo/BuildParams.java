package com.riil.build.pojo;

import java.io.File;
import java.util.Set;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

/**
 * User: wangchongyang on 2017/9/13 0013.
 */
public class BuildParams {
    private String neo4jDbPath;
    private Set<BasicBuildRelationPojo> relationPojos;
    private RelationshipType relationshipType;
    private Label label;
    private boolean isAggregate;
    private File specifyPath;

    public String getNeo4jDbPath() {
        return neo4jDbPath;
    }

    public void setNeo4jDbPath(final String neo4jDbPath) {
        this.neo4jDbPath = neo4jDbPath;
    }

    public Set<BasicBuildRelationPojo> getRelationPojos() {
        return relationPojos;
    }

    public void setRelationPojos(final Set<BasicBuildRelationPojo> relationPojos) {
        this.relationPojos = relationPojos;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(final RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(final Label label) {
        this.label = label;
    }

    public boolean isAggregate() {
        return isAggregate;
    }

    public void setAggregate(final boolean aggregate) {
        isAggregate = aggregate;
    }

    public File getSpecifyPath() {
        return specifyPath;
    }

    public void setSpecifyPath(final File specifyPath) {
        this.specifyPath = specifyPath;
    }
}
