package com.riil.build.services;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.riil.build.pojo.JarDirectPojo;
import com.riil.build.pojo.JarPojo;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class GenJarBuildOrderServiceTest {
    public static final String TRUNK_SOURCE = "\\trunk\\source";


    public static final String RIIL_WEBFRAMEWORK_PATH = "E:\\codeSVN\\RIIL_WebFramework";
    private static final String super3rdPom = "D:\\m2_repository\\com\\riil\\super.3rd.pom\\6.8.7-SNAPSHOT\\super.3rd.pom-6.8.7-SNAPSHOT.pom";
    private static final String RIIL_BMC_ADMINCONSOLE_PATH = "E:\\codeSVN\\RIIL_BMC_AdminConsole";
    private static final String EAC_ACTION_POM_PATH = "E:\\codeSVN\\RIIL_BMC_EAC\\trunk\\source\\riil-eac-action\\pom.xml";
    private static final String NEO4J_DATA_PATH = "E:\\works\\neo4j\\neo4j.db";
    private static final String RIIL_BMC_FRAMEWORK_PATH = "E:\\codeSVN\\RIIL_WebFramework";
    private static GenJarBuildOrderService buildOrderService;

    @BeforeClass
    public static void newGenJarBuildOrderService() {
        buildOrderService = BuildOrderServiceBuilder.INSTANCE.newGenJarBuildOrderService();
    }

    @Test
    public void register3rdJar() throws Exception {
        buildOrderService.register3rdJar(super3rdPom);
    }

    @Test
    public void getPomsBySpecifyRange() throws Exception {
        final List<File> pomList = buildOrderService.getPomsBySpecifyRange(RIIL_BMC_ADMINCONSOLE_PATH + TRUNK_SOURCE);
        for (File file : pomList) {
            System.out.println(file.getCanonicalPath()/*+":"+file.getParent()*/);
        }
    }

    @Test
    public void getDirectDependencyByPom() throws Exception {
        final JarDirectPojo jarDirectPojo = buildOrderService.getDirectDependencyByPom(EAC_ACTION_POM_PATH);
        System.out.println(jarDirectPojo);
    }

    @Test
    public void getJarBuildOrder() throws Exception {
        List<JarDirectPojo> jarDirectPojos = mockJarDirectPojoList();
        final List<Set<JarPojo>> jarBuildOrder = buildOrderService.getJarBuildOrder(jarDirectPojos, NEO4J_DATA_PATH);
        System.out.println(jarBuildOrder);
    }

    private List<JarDirectPojo> mockJarDirectPojoList() {
        List<JarDirectPojo> jarDirectPojos = Lists.newArrayList();
        Set<String> nodeNameSet = ImmutableSet.of("A", "B", "C", "D", "E", "F", "G", "H", "I");
        Map<String, Set<String>> nodeRelationMap = Maps.newHashMap();
        nodeRelationMap.put("A", ImmutableSet.of("B", "G"));
        nodeRelationMap.put("B", ImmutableSet.of("C", "F"));
        nodeRelationMap.put("C", ImmutableSet.of("D"));
        nodeRelationMap.put("D", ImmutableSet.of("E"));
        nodeRelationMap.put("E", Sets.<String>newHashSet());
        nodeRelationMap.put("F", Sets.<String>newHashSet());
        nodeRelationMap.put("G", ImmutableSet.of("D"));
        nodeRelationMap.put("H", ImmutableSet.of("C", "G", "I"));
        nodeRelationMap.put("I", ImmutableSet.of("E"));
        Map<String, JarDirectPojo> jarDirectPojoMap = Maps.newHashMap();
        for (String nodeName : nodeNameSet) {
            final JarDirectPojo jarDirectPojo = genJarDirectPojo(nodeName);
            jarDirectPojoMap.put(nodeName, jarDirectPojo);
        }
        for (Map.Entry<String, Set<String>> setEntry : nodeRelationMap.entrySet()) {
            final Set<String> dependencyNodes = setEntry.getValue();
            Set<JarPojo> jarDirectPojoSet = Sets.newHashSet();
            for (String nodeName : dependencyNodes) {
                jarDirectPojoSet.add(jarDirectPojoMap.get(nodeName));
            }
            jarDirectPojoMap.get(setEntry.getKey()).setDirectDependency(jarDirectPojoSet);
        }
        jarDirectPojos.addAll(jarDirectPojoMap.values());
        return jarDirectPojos;
    }

    private JarDirectPojo genJarDirectPojo(String nodeName) {
        final JarDirectPojo jarDirectPojo = new JarDirectPojo();
        jarDirectPojo.setGroupId("groupId-" + nodeName);
        jarDirectPojo.setArtifactId("artifactId-" + nodeName);
        jarDirectPojo.setVersion("version-" + nodeName);
        return jarDirectPojo;
    }

}
