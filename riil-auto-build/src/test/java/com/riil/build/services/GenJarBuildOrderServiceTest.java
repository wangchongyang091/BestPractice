package com.riil.build.services;

import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.riil.build.pojo.BasicBuildPojo;
import com.riil.build.pojo.BasicBuildRelationPojo;
import com.riil.build.pojo.BuildParams;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class GenJarBuildOrderServiceTest {

    public static final List<String> FRONT_END_REPOSITORY = ImmutableList.of("E:\\codeSVN\\RIIL_BMC_Adapter", "E:\\codeSVN\\RIIL_BMC_AdminConsole", "E:\\codeSVN\\RIIL_BMC_Business", "E:\\codeSVN\\RIIL_BMC_EAC", "E:\\codeSVN\\RIIL_BMC_Education", "E:\\codeSVN\\RIIL_BMC_Knowledge", "E:\\codeSVN\\RIIL_BMC_LogMonitor", "E:\\codeSVN\\RIIL_BMC_NetTopo", "E:\\codeSVN\\RIIL_BMC_OneTopo", "E:\\codeSVN\\RIIL_BMC_Report", "E:\\codeSVN\\RIIL_BMC_ResMonitor", "E:\\codeSVN\\RIIL_BMC_Util", "E:\\codeSVN\\RIIL_RMC_CMDB", "E:\\codeSVN\\RIIL_WebFramework");
    private static final String OUTPUT_PATH = "F:\\RIIL_WebFramework\\build_order.json";
    private static final String RIIL_WEBFRAMEWORK_PATH = "E:\\codeSVN\\RIIL_WebFramework";
    private static final String TRUNK_SOURCE = "\\trunk\\source";
    private static final String superRiilPomPath = "D:\\m2_repository\\com\\riil\\super.riil.pom\\6.8.7-SNAPSHOT\\super.riil.pom-6.8.7-SNAPSHOT.pom";
    private static final String super3rdPomPath = "D:\\m2_repository\\com\\riil\\super.3rd.pom\\6.8.7-SNAPSHOT\\super.3rd.pom-6.8.7-SNAPSHOT.pom";
    private static final String RIIL_BMC_ADMINCONSOLE_PATH = "E:\\codeSVN\\RIIL_BMC_AdminConsole";
    private static final String EAC_ACTION_POM_PATH = "E:\\codeSVN\\RIIL_BMC_EAC\\trunk\\source\\riil-eac-action\\pom.xml";
    private static final String NEO4J_DATA_PATH = "E:\\works\\neo4j\\neo4j.db";
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
        List<String> codeModulesPath = ImmutableList.of("E:\\codeSVN\\RIIL_BMC_Adapter", "E:\\codeSVN\\RIIL_BMC_AdminConsole", "E:\\codeSVN\\RIIL_BMC_Business", "E:\\codeSVN\\RIIL_BMC_EAC", "E:\\codeSVN\\RIIL_BMC_Education", "E:\\codeSVN\\RIIL_BMC_Knowledge", "E:\\codeSVN\\RIIL_BMC_LogMonitor", "E:\\codeSVN\\RIIL_BMC_NetTopo", "E:\\codeSVN\\RIIL_BMC_OneTopo", "E:\\codeSVN\\RIIL_BMC_Report", "E:\\codeSVN\\RIIL_BMC_ResMonitor", "E:\\codeSVN\\RIIL_BMC_Util", "E:\\codeSVN\\RIIL_RMC_CMDB", "E:\\codeSVN\\RIIL_WebFramework");
        List<File> pomFiles = Lists.newArrayList();
        for (String codeModulePath : codeModulesPath) {
            final File specifyFile = new File(codeModulePath + TRUNK_SOURCE);
            final List<File> pomList = buildOrderService.getPomsBySpecifyRange(specifyFile);
            pomFiles.addAll(pomList);
        }
        for (File file : pomFiles) {
            System.out.println(file.getCanonicalPath());
        }
        System.out.println("Total poms=>" + pomFiles.size());
    }

    @Test
    public void genProjectBuildPathRegister() throws Exception {
        final File specifyFile = new File(RIIL_WEBFRAMEWORK_PATH + TRUNK_SOURCE);
        final List<File> pomFiles = buildOrderService.getPomsBySpecifyRange(specifyFile);
        final Map<String, String> buildPathRegister = buildOrderService.genProjectBuildPathRegister(pomFiles);
        System.out.println(buildPathRegister);
    }

    @Test
    public void aggregatePomRegister() throws Exception {
        final File riilWebFrameworkFile = new File(RIIL_WEBFRAMEWORK_PATH + TRUNK_SOURCE);
        final List<File> pomFiles = buildOrderService.getPomsBySpecifyRange(riilWebFrameworkFile);
        final Map<String, Set<String>> aggregatePomRegister = buildOrderService.aggregatePomRegister(pomFiles);
        System.out.println(aggregatePomRegister);
    }


    @Test
    public void genBuildRelations() throws Exception {
        final File riilWebFrameworkFile = new File(RIIL_WEBFRAMEWORK_PATH + TRUNK_SOURCE);
        final List<File> pomList = buildOrderService.getPomsBySpecifyRange(riilWebFrameworkFile);
        final Set<BasicBuildRelationPojo> relationPojos = buildOrderService.genBuildRelations(pomList);
        System.out.println(relationPojos);
    }

    @Test
    public void addData2Graph() throws Exception {
        List<File> pomFiles = Lists.newArrayList();
        for (String codeModulePath : FRONT_END_REPOSITORY) {
            final File specifyFile = new File(codeModulePath + TRUNK_SOURCE);
            final List<File> pomList = buildOrderService.getPomsBySpecifyRange(specifyFile);
            pomFiles.addAll(pomList);
        }
        System.out.println("pom files=>" + pomFiles.size());
        boolean isReal = true;
        Set<BasicBuildRelationPojo> relationPojos = mockJarDirectPojoList();
        if (isReal) {
            relationPojos = buildOrderService.genBuildRelations(pomFiles);
        }
        System.out.println("build relations=>" + relationPojos.size());
        final BuildParams buildParams = new BuildParams();
        buildParams.setNeo4jDbPath(NEO4J_DATA_PATH);
        buildParams.setLabel(GenJarBuildOrderService.MyLabels.BASICBUILD);
        buildParams.setRelationshipType(GenJarBuildOrderService.MyRelationshipTypes.AFTER);
        buildParams.setRelationPojos(relationPojos);
        buildOrderService.addData2Graph(buildParams);
    }

    @Test
    public void getJarBuildOrder() throws Exception {
        List<File> pomFiles = Lists.newArrayList();
        for (String codeModulePath : FRONT_END_REPOSITORY) {
            final File specifyFile = new File(codeModulePath + TRUNK_SOURCE);
            final List<File> pomList = buildOrderService.getPomsBySpecifyRange(specifyFile);
            pomFiles.addAll(pomList);
        }
        buildOrderService.aggregatePomRegister(pomFiles);
        final BuildParams buildParams = new BuildParams();
        buildParams.setNeo4jDbPath(NEO4J_DATA_PATH);
        buildParams.setRelationshipType(GenJarBuildOrderService.MyRelationshipTypes.AFTER);
        buildParams.setLabel(GenJarBuildOrderService.MyLabels.BASICBUILD);
        buildParams.setAggregate(true);
        final List<Set<BasicBuildPojo>> buildOrder = buildOrderService.getJarBuildOrder(buildParams);
        final String buildOrderJsonStr = JSONArray.toJSON(buildOrder).toString();
        Files.write(buildOrderJsonStr, new File(OUTPUT_PATH), Charsets.UTF_8);
        System.out.println("output OK!");
    }

    @Test
    public void mapSort() {
        Map<String, Set<String>> aggregateMap = Maps.newHashMap();
        aggregateMap.put("A", Sets.newHashSet("B", "C", "E", "F"));
        aggregateMap.put("H", Sets.<String>newHashSet("G", "J"));
        aggregateMap.put("B", Sets.newHashSet("H", "I"));
        System.out.println(aggregateMap);
        final List<Map.Entry<String, Set<String>>> aggregateEntrys = Lists.newArrayList(aggregateMap.entrySet());
        Collections.sort(aggregateEntrys, new Comparator<Map.Entry<String, Set<String>>>() {
            @Override
            public int compare(final Map.Entry<String, Set<String>> e1, final Map.Entry<String, Set<String>> e2) {
                return e1.getValue().contains(e2.getKey()) ? 1 : -1;
            }
        });
        System.out.println(aggregateEntrys);
    }

    @Test
    public void subString() throws Exception {
        final String buildPathDirectParent = "E:\\codeSVN\\RIIL_BMC_OneTopo\\trunk\\source\\riil-onetopo-core-api";
        final String[] split = StringUtils.split(buildPathDirectParent, "\\\\");
        for (String s : split) {
            System.out.println(s);
        }
    }

    @Test
    public void write2File() throws IOException {
        Files.write("hello", new File("F:\\RIIL_WebFramework\\test.json"), Charsets.UTF_8);
        System.out.println("Write Success!");
    }

    @Test
    public void getMinLength() {
        int lengths[] = {5, 3, 2, 6, 1, 7, 4, 7, 10};
        Arrays.sort(lengths);
        System.out.println(lengths[0] + ":" + lengths[lengths.length - 1]);
        List<Integer> lengthSet = Lists.newArrayList(7, 3, 4, 2, 8, 1, 5, 23);
        Collections.sort(lengthSet, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return i1 < i2 ? 1 : -1;
            }
        });
        System.out.println(lengthSet);
    }

    private Set<BasicBuildRelationPojo> mockJarDirectPojoList() {
        Set<BasicBuildRelationPojo> relationPojos = Sets.newHashSet();
        Set<String> nodeNameSet = ImmutableSet.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
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
        nodeRelationMap.put("J", ImmutableSet.of("K"));
        nodeRelationMap.put("L", Sets.<String>newHashSet());
        Map<String, BasicBuildRelationPojo> relationPojoMap = Maps.newHashMap();
        for (String nodeName : nodeNameSet) {
            final BasicBuildRelationPojo jarDirectPojo = getBuildRelation(nodeName);
            relationPojoMap.put(nodeName, jarDirectPojo);
        }
        for (Map.Entry<String, Set<String>> setEntry : nodeRelationMap.entrySet()) {
            final Set<String> beforeBuilds = setEntry.getValue();
            Set<BasicBuildPojo> beforeBuildPojos = Sets.newHashSet();
            if (CollectionUtils.isNotEmpty(beforeBuilds)) {
                for (String nodeName : beforeBuilds) {
                    beforeBuildPojos.add(relationPojoMap.get(nodeName));
                }
            }
            relationPojoMap.get(setEntry.getKey()).setBeforeBuilds(beforeBuildPojos);
        }
        relationPojos.addAll(relationPojoMap.values());
        return relationPojos;
    }

    private BasicBuildRelationPojo getBuildRelation(String buildPath) {
        final BasicBuildRelationPojo jarDirectPojo = new BasicBuildRelationPojo();
        jarDirectPojo.setGroupId("groupId-" + buildPath);
        jarDirectPojo.setArtifactId("artifactId-" + buildPath);
        jarDirectPojo.setBuildPath(buildPath);
        return jarDirectPojo;
    }

}
