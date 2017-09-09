package com.riil.build.pojo;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class JarDirectPojo extends JarPojo {
    private Set<JarPojo> directDependency = Sets.newHashSet();

    public Set<JarPojo> getDirectDependency() {
        return directDependency;
    }

    public void setDirectDependency(final Set<JarPojo> directDependency) {
        this.directDependency = directDependency;
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s\n%s\ndirectDependencySize=%d", this.getGroupId(), this.getArtifactId(), this.getVersion(), directToString(), getDirectDependency().size());
    }

    private List<String> directToString() {
        final Set<JarPojo> directDependency = getDirectDependency();
        List<String> artifactIds = Lists.newArrayList();
        for (JarPojo jarPojo : directDependency) {
            artifactIds.add(String.format("\n  %s:%s:%s", jarPojo.getGroupId(), jarPojo.getArtifactId(), jarPojo.getVersion()));
        }
        return artifactIds;
    }
}
