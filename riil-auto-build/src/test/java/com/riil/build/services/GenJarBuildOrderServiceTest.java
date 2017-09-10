package com.riil.build.services;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.riil.build.pojo.BasicBuildPojo;
import com.riil.build.pojo.BasicBuildRelationPojo;
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


    private static final String RIIL_WEBFRAMEWORK_PATH = "E:\\codeSVN\\RIIL_WebFramework";
    private static final String TRUNK_SOURCE = "\\trunk\\source";
    private static final String superRiilPomPath = "D:\\m2_repository\\com\\riil\\super.riil.pom\\6.8.7-SNAPSHOT\\super.riil.pom-6.8.7-SNAPSHOT.pom";
    private static final String super3rdPomPath = "D:\\m2_repository\\com\\riil\\super.3rd.pom\\6.8.7-SNAPSHOT\\super.3rd.pom-6.8.7-SNAPSHOT.pom";
    private static final String RIIL_BMC_ADMINCONSOLE_PATH = "E:\\codeSVN\\RIIL_BMC_AdminConsole";
    private static final String EAC_ACTION_POM_PATH = "E:\\codeSVN\\RIIL_BMC_EAC\\trunk\\source\\riil-eac-action\\pom.xml";
    private static final String NEO4J_DATA_PATH = "E:\\works\\neo4j\\neo4j.db";
    private static final String RIIL_BMC_FRAMEWORK_PATH = "E:\\codeSVN\\RIIL_WebFramework";
    private static final String riilWebDependencyPomPath = "D:\\m2_repository\\com\\riil\\webapp\\riil.web.dependency\\6.8.7-SNAPSHOT\\riil.web.dependency-6.8.7-SNAPSHOT.pom";
    private static GenJarBuildOrderService buildOrderService;

    @BeforeClass
    public static void newGenJarBuildOrderService() {
        buildOrderService = BuildOrderServiceBuilder.INSTANCE.newGenJarBuildOrderService();
    }

    @Test
    public void registerSuperJar() throws Exception {
        buildOrderService.registerSuperRiilJar(superRiilPomPath);
        buildOrderService.registerSuper3rdJar(riilWebDependencyPomPath);
        System.out.println(GenJarBuildOrderService.superJarRegister.values() + "\nsize=" + GenJarBuildOrderService.superJarRegister.size());
    }

    @Test
    public void getPomsBySpecifyRange() throws Exception {
        final List<File> pomList = buildOrderService.getPomsBySpecifyRange(RIIL_WEBFRAMEWORK_PATH + TRUNK_SOURCE);
        for (File file : pomList) {
            System.out.println(file.getCanonicalPath() + ":" + file.getParent());
        }
    }

    @Test
    public void genProjectBuildPathRegister() throws Exception {
        final Map<String, String> buildPathRegister = buildOrderService.genProjectBuildPathRegister(buildOrderService.getPomsBySpecifyRange(RIIL_WEBFRAMEWORK_PATH + TRUNK_SOURCE));
        System.out.println(buildPathRegister);
    }

    @Test
    public void getDirectDependencyByPom() throws Exception {
        final List<File> pomList = buildOrderService.getPomsBySpecifyRange(RIIL_WEBFRAMEWORK_PATH + TRUNK_SOURCE);
        final Set<BasicBuildRelationPojo> relationPojos = buildOrderService.getBuildRelations(pomList);
        System.out.println(relationPojos);
    }

    @Test
    public void getJarBuildOrder() throws Exception {
        Set<BasicBuildRelationPojo> jarDirectPojos = mockJarDirectPojoList();
        final List<Set<BasicBuildPojo>> jarBuildOrder = buildOrderService.getJarBuildOrder(jarDirectPojos, NEO4J_DATA_PATH);
        System.out.println(jarBuildOrder);
    }

    private Set<BasicBuildRelationPojo> mockJarDirectPojoList() {
        Set<BasicBuildRelationPojo> jarDirectPojos = Sets.newHashSet();
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
        Map<String, BasicBuildRelationPojo> jarDirectPojoMap = Maps.newHashMap();
        for (String nodeName : nodeNameSet) {
            final BasicBuildRelationPojo jarDirectPojo = getBuildRelation(nodeName);
            jarDirectPojoMap.put(nodeName, jarDirectPojo);
        }
        for (Map.Entry<String, Set<String>> setEntry : nodeRelationMap.entrySet()) {
            final Set<String> dependencyNodes = setEntry.getValue();
            Set<BasicBuildPojo> jarDirectPojoSet = Sets.newHashSet();
            for (String nodeName : dependencyNodes) {
                jarDirectPojoSet.add(jarDirectPojoMap.get(nodeName));
            }
            jarDirectPojoMap.get(setEntry.getKey()).setBeforeBuilds(jarDirectPojoSet);
        }
        jarDirectPojos.addAll(jarDirectPojoMap.values());
        return jarDirectPojos;
    }

    private BasicBuildRelationPojo getBuildRelation(String buildPath) {
        final BasicBuildRelationPojo jarDirectPojo = new BasicBuildRelationPojo();
        jarDirectPojo.setBuildPath(buildPath);
        return jarDirectPojo;
    }

}
