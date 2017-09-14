package com.riil.build.pojo;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class BasicBuildPojo {
    private String groupId;
    private String artifactId;
    private String buildPath;

    public BasicBuildPojo(final String groupId, final String artifactId, final String buildPath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.buildPath = buildPath;
    }

    public BasicBuildPojo() {

    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getBuildPath() {
        return buildPath;
    }

    public void setBuildPath(final String buildPath) {
        this.buildPath = buildPath;
    }

    @Override
    public String toString() {
        return "artifactId=>" + artifactId;
    }
}
