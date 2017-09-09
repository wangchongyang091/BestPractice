package com.riil.build.pojo;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class JarPojo {
    private String groupId;
    private String artifactId;
    private String version;


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

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
//        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        return "JarPojo{" +
//                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
//                ", version='" + version + '\'' +
                '}';
    }
}
