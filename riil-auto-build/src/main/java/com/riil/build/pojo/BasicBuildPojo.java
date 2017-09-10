package com.riil.build.pojo;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class BasicBuildPojo {
    private String buildPath;

    public BasicBuildPojo(final String buildPath) {
        this.buildPath = buildPath;
    }

    public BasicBuildPojo() {

    }

    public String getBuildPath() {
        return buildPath;
    }

    public void setBuildPath(final String buildPath) {
        this.buildPath = buildPath;
    }

    @Override
    public String toString() {
        return "buildPath=>" + buildPath;
    }
}
