package com.riil.build.pojo;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class BasicBuildRelationPojo extends BasicBuildPojo {
    private Set<BasicBuildPojo> beforeBuilds = Sets.newHashSet();

    public Set<BasicBuildPojo> getBeforeBuilds() {
        return beforeBuilds;
    }

    public void setBeforeBuilds(final Set<BasicBuildPojo> beforeBuilds) {
        this.beforeBuilds = beforeBuilds;
    }

    @Override
    public String toString() {
//        return String.format("%s:%s:%s\n%s\ndirectDependencySize=%d", this.getGroupId(), this.getArtifactId(), this.getVersion(), beforeBuilds(), getBeforeBuilds().size());
        return this.getBuildPath() + "\nbefore build=>" + beforeBuilds();
    }


    private List<String> beforeBuilds() {
        final Set<BasicBuildPojo> directDependency = getBeforeBuilds();
        List<String> buildPaths = Lists.newArrayList();
        for (BasicBuildPojo basicBuildPojo : directDependency) {
//            buildPaths.add(String.format("\n  %s:%s:%s", basicBuildPojo.getGroupId(), basicBuildPojo.getArtifactId(), basicBuildPojo.getVersion()));
            buildPaths.add(String.format("\n  %s", basicBuildPojo.getBuildPath()));
        }
        return buildPaths;
    }
}
