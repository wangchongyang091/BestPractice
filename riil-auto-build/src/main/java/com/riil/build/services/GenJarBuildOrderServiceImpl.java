package com.riil.build.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.riil.build.exception.GenDependencyRealtionException;
import com.riil.build.exception.ParsePomException;
import com.riil.build.pojo.BasicBuildPojo;
import com.riil.build.pojo.BasicBuildRelationPojo;
import com.riil.build.pojo.BuildParams;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
class GenJarBuildOrderServiceImpl implements GenJarBuildOrderService {
    public static final String MODULE = "module";
    private static final SAXReader SAX_READER = new SAXReader();
    private static final String ARTIFACT_ID = "artifactId";
    private static final String GROUP_ID = "groupId";
    private static final String BUILD_PATH = "buildPath";
    private static final String PARENT = "parent";
    private static final String MODULES = "modules";
    private GraphDatabaseService graphDb;


    @Override
    public void registerSuperRiilJar(final String superRiilPomPath) throws ParsePomException {
//        registerSuperJar(superRiilPomPath);
    }

    @Override
    public void registerSuper3rdJar(final String super3rdPomPath) throws ParsePomException {
//        registerSuperJar(super3rdPomPath);
    }

    @Override
    public List<File> getPomsBySpecifyRange(final File parseFile) throws ParsePomException {
        try {
            List<File> pomList = Lists.newArrayList();
            return getPomFiles(pomList, parseFile);
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
    }

    @Override
    public Map<String, Set<String>> aggregatePomRegister(final List<File> pomFiles) throws ParsePomException {
        try {
//            Map<String, Set<String>> modulesRegister = Maps.newHashMap();
            for (File pomFile : pomFiles) {
                final Document document = SAX_READER.read(pomFile);
                final Element projectEle = document.getRootElement();
                String groupId = projectEle.elementTextTrim(GROUP_ID);
                final String artifactId = projectEle.elementTextTrim(ARTIFACT_ID);
                if (StringUtils.isBlank(groupId)) {
                    groupId = projectEle.element(PARENT).elementTextTrim(GROUP_ID);
                }
                final Element modulesEle = projectEle.element(MODULES);
                if (null != modulesEle) {
                    final Iterator moduleEles = modulesEle.elementIterator(MODULE);
                    Set<String> moduleSet = Sets.newHashSet();
                    while (moduleEles.hasNext()) {
                        final Element moduleEle = (Element) moduleEles.next();
                        final String module = moduleEle.getTextTrim();
                        if (StringUtils.isNotBlank(module)) {
                            moduleSet.add(module);
                        }
                    }
                    aggregatePomRegister.put(artifactId, moduleSet);
                }
            }
            return aggregatePomRegister;
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
    }

    @Override
    public Map<String, String> genProjectBuildPathRegister(final List<File> pomFiles) throws ParsePomException {
        try {
            Map<String, String> buildPathRegister = Maps.newHashMap();
            for (File pomFile : pomFiles) {
                final Document document = SAX_READER.read(pomFile);
                final Element projectEle = document.getRootElement();
                String groupId = projectEle.elementTextTrim(GROUP_ID);
                final String artifactId = projectEle.elementTextTrim(ARTIFACT_ID);
                if (StringUtils.isBlank(groupId)) {
                    groupId = projectEle.element(PARENT).elementTextTrim(GROUP_ID);
                }
                buildPathRegister.put(String.format("%s:%s", groupId, artifactId), pomFile.getParent());
            }
            return buildPathRegister;
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
    }

    @Override
    public Set<BasicBuildRelationPojo> genBuildRelations(final List<File> pomFiles) throws ParsePomException {
        final Map<String, String> buildPathRegister = genProjectBuildPathRegister(pomFiles);
        Set<BasicBuildRelationPojo> relationPojoSet = Sets.newHashSet();
        try {
            for (File pomFile : pomFiles) {
                relationPojoSet.add(GenBuildRelationByPom(buildPathRegister, pomFile));
            }
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
        return relationPojoSet;
    }

    private BasicBuildRelationPojo GenBuildRelationByPom(final Map<String, String> buildPathRegister, final File pomFile) throws DocumentException {
        final Document document = SAX_READER.read(pomFile);
        final Element projectEle = document.getRootElement();
        final Element parentEle = projectEle.element(PARENT);
        Set<BasicBuildPojo> beforeBuildPojos = Sets.newHashSet();
//                add parent before
        String parentGroupId = null;
        if (null != parentEle) {
            parentGroupId = parentEle.elementTextTrim(GROUP_ID);
            final String parentArtifactId = parentEle.elementTextTrim(ARTIFACT_ID);
            beforeBuildPojos = Sets.newHashSet();
            addBeforeBuildPath(buildPathRegister, beforeBuildPojos, parentGroupId, parentArtifactId);
        }
        final BasicBuildRelationPojo relationPojo = new BasicBuildRelationPojo();
        relationPojo.setBuildPath(pomFile.getParent());
        final String groupId = projectEle.elementTextTrim(GROUP_ID);
        final String artifactId = projectEle.elementTextTrim(ARTIFACT_ID);
        relationPojo.setArtifactId(artifactId);
        relationPojo.setGroupId(StringUtils.isBlank(groupId) ? parentGroupId : groupId);
        final Element dependencyManagementEle = projectEle.element("dependencyManagement");
        if (null != dependencyManagementEle) {
            final Iterator dependencyEles = dependencyManagementEle.element("dependencies").elementIterator("dependency");
            while (dependencyEles.hasNext()) {
                final Element dependencyEle = (Element) dependencyEles.next();
                if ("pom".equals(dependencyEle.elementTextTrim("type"))) {
                    addBeforeBuildPath(buildPathRegister, beforeBuildPojos, dependencyEle.elementTextTrim(GROUP_ID), dependencyEle.elementTextTrim(ARTIFACT_ID));
                }
            }
        }
        final Element dependenciesEle = projectEle.element("dependencies");
        if (null != dependenciesEle) {
            final Iterator dependencyEles = dependenciesEle.elementIterator("dependency");
            while (dependencyEles.hasNext()) {
                final Element dependencyEle = (Element) dependencyEles.next();
                addBeforeBuildPath(buildPathRegister, beforeBuildPojos, dependencyEle.elementTextTrim(GROUP_ID), dependencyEle.elementTextTrim(ARTIFACT_ID));
            }
        }
        relationPojo.setBeforeBuilds(beforeBuildPojos);
        return relationPojo;
    }

    @Override
    public void addData2Graph(BuildParams buildParams) {
//        FileUtils.deleteRecursively(DB_PATH);
//        transaction
        startDb(buildParams.getNeo4jDbPath());
        try (Transaction tx = graphDb.beginTx()) {
//            addData
            addData(buildParams.getRelationPojos(), buildParams.getRelationshipType(), buildParams.getLabel());
            tx.success();
        }
    }

    @Override
    public List<Set<BasicBuildPojo>> getJarBuildOrder(BuildParams buildParams) throws GenDependencyRealtionException {
        try {
            startDb(buildParams.getNeo4jDbPath());
            final Map<Node, Integer> nodeIntegerMap = generateNodeLongestPath(buildParams.getLabel(), buildParams.getRelationshipType());
            System.out.println("node length=>" + nodeIntegerMap.values().size());
            if (buildParams.isAggregate()) {
                processAggregateNode(nodeIntegerMap);
            }
            System.out.println("node length after process=>" + nodeIntegerMap.values().size());
            final List<Set<Node>> nodeBuildList = convert2buildSort(nodeIntegerMap);
            System.out.printf("build sort=>%s%n", nodeBuildList);
            return covert2JarList(nodeBuildList);
        } catch (Exception e) {
            throw new GenDependencyRealtionException(e);
        }
    }

    private void processAggregateNode(final Map<Node, Integer> nodeLengthMap) {
        Map<String, Node> nodeMap = registerNode(nodeLengthMap);
//        给聚合模块排序，例如{A=[B, C, E, F], B=[H, I], H=[J, G]}排序后为[H=[J, G], B=[H, I], A=[B, C, E, F]]
        final List<Map.Entry<String, Set<String>>> aggregateEntrys = Lists.newArrayList(aggregatePomRegister.entrySet());
        Collections.sort(aggregateEntrys, new Comparator<Map.Entry<String, Set<String>>>() {
            @Override
            public int compare(final Map.Entry<String, Set<String>> e1, final Map.Entry<String, Set<String>> e2) {
                return e1.getValue().contains(e2.getKey()) ? 1 : -1;
            }
        });
        for (Map.Entry<String, Set<String>> aggregateEntry : aggregateEntrys) {
            final Set<String> childModules = aggregateEntry.getValue();
            final String aggregateModule = aggregateEntry.getKey();
            Set<String> modules = Sets.newHashSet();
            modules.addAll(childModules);
            modules.add(aggregateModule);
            final Integer modulesMinLength = calculateAggregateModuleMinLength(nodeLengthMap, nodeMap, modules);
            resetAggregateNodeLength(nodeLengthMap, nodeMap, aggregateModule, modulesMinLength);
            removeChildNodes(nodeLengthMap, nodeMap, childModules);
        }
    }

    private void removeChildNodes(final Map<Node, Integer> nodeLengthMap, final Map<String, Node> nodeMap, final Set<String> childModules) {
        for (String module : childModules) {
            final String realKey = getModuleRegisterRealKey(nodeMap, module);
            nodeLengthMap.remove(nodeMap.get(realKey));
        }
    }

    private void resetAggregateNodeLength(final Map<Node, Integer> nodeLengthMap, final Map<String, Node> nodeMap, final String module, final Integer modulesMinLength) {
        final String realKey = getModuleRegisterRealKey(nodeMap, module);
        nodeLengthMap.put(nodeMap.get(realKey), modulesMinLength);
    }

    private Integer calculateAggregateModuleMinLength(final Map<Node, Integer> nodeLengthMap, final Map<String, Node> nodeMap, final Set<String> modules) {
        List<Integer> modulesLengthList = Lists.newArrayList();
        for (String module : modules) {
            String realKey = getModuleRegisterRealKey(nodeMap, module);
            final Node moduleNodes = nodeMap.get(realKey);
            modulesLengthList.add(nodeLengthMap.get(moduleNodes));
        }
        Collections.sort(modulesLengthList, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return i1 < i2 ? -1 : 1;
            }
        });
        return modulesLengthList.get(0);
    }

    private String getModuleRegisterRealKey(final Map<String, Node> nodeMap, final String module) {
        final Set<String> keySet = nodeMap.keySet();
        String realKey = module;
        System.out.println(module);
        for (String key : keySet) {
            if (key.contains(module)) {
                realKey = key;
                System.out.println(key);
                break;
            }
        }
        return realKey;
    }

    private Map<String, Node> registerNode(final Map<Node, Integer> nodeLengthMap) {
        Map<String, Node> nodeMap = Maps.newHashMap();
        for (Map.Entry<Node, Integer> entry : nodeLengthMap.entrySet()) {
            final Node node = entry.getKey();
            final String buildPath = (String) node.getProperty(BUILD_PATH);
            final String[] splitDir = StringUtils.split(buildPath, "\\\\");
            final String key = splitDir[splitDir.length - 1];
//            由于RIIL中的maven项目POM文件的写法很多都不是官方推荐值(如artifactId与直接项目路径不符)，故采用artifactId和直接项目路径拼接为KEY
            nodeMap.put(String.format("%s:%s", node.getProperties(ARTIFACT_ID), key), node);
            System.out.printf("%s:%s:%s%n", node.getProperty(GROUP_ID), node.getProperty(ARTIFACT_ID), buildPath);
        }
        return nodeMap;
    }

    private List<File> getPomFiles(List<File> fileList, final File parseFile) throws IOException {
        if (parseFile.exists()) {
            if (parseFile.isDirectory()) {
                final File[] files = parseFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        getPomFiles(fileList, file);
                    }
                }
            } else if ("pom.xml".equalsIgnoreCase(parseFile.getName())) {
                fileList.add(parseFile);
            }
        }
        return fileList;
    }

    private List<Set<BasicBuildPojo>> covert2JarList(final List<Set<Node>> buildNodeList) {
        List<Set<BasicBuildPojo>> buildJarList = Lists.newArrayList();
        for (Set<Node> nodes : buildNodeList) {
            Set<BasicBuildPojo> basicBuildPojoSet = Sets.newHashSet();
            for (Node node : nodes) {
                basicBuildPojoSet.add(new BasicBuildPojo((String) node.getProperty(GROUP_ID), (String) node.getProperty(ARTIFACT_ID), (String) node.getProperty(BUILD_PATH)));
            }
            buildJarList.add(basicBuildPojoSet);
        }
        return buildJarList;
    }


    private void addBeforeBuildPath(final Map<String, String> buildPathRegister, final Set<BasicBuildPojo> beforeBuildPojos, final String groupId, final String artifactId) {
        String buildPath;
        if (null != groupId) {
            buildPath = buildPathRegister.get(String.format("%s:%s", groupId, artifactId));
        } else {
            final Set<String> keySet = buildPathRegister.keySet();
            String replaceKey = null;
            for (String key : keySet) {
                if (key.contains(artifactId)) {
                    replaceKey = key;
                    break;
                }
            }
            buildPath = buildPathRegister.get(replaceKey);
        }
        if (null != buildPath) {
            beforeBuildPojos.add(new BasicBuildPojo(groupId, artifactId, buildPath));
        }
    }

    private void startDb(String neo4jDbPath) {
        //        startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jDbPath));
        registerShutdownHook(graphDb);
    }

    private void addData(Set<BasicBuildRelationPojo> relationPojos, RelationshipType relationshipType, Label label) {
        if (CollectionUtils.isNotEmpty(relationPojos)) {
            Map<Node, Set<Node>> nodeRelationMap = Maps.newHashMap();
            Map<String, Node> nodeRegister = Maps.newHashMap();
            for (BasicBuildRelationPojo buildRelation : relationPojos) {
                String nodeMark = buildRelation.getBuildPath();
//                System.out.println(nodeMark);
                Node currentNode = getNode(nodeRegister, buildRelation, nodeMark, label);
                final Set<BasicBuildPojo> jarDepSet = buildRelation.getBeforeBuilds();
                Set<Node> beforeNodes = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(jarDepSet)) {
                    for (BasicBuildPojo build : jarDepSet) {
                        nodeMark = build.getBuildPath();
                        Node beforeNode = getNode(nodeRegister, build, nodeMark, label);
                        beforeNodes.add(beforeNode);
                    }
                }
                nodeRelationMap.put(currentNode, beforeNodes);
            }
            for (Map.Entry<Node, Set<Node>> nodeSetEntry : nodeRelationMap.entrySet()) {
                final Set<Node> depNodes = nodeSetEntry.getValue();
                final Node curNode = nodeSetEntry.getKey();
                if (CollectionUtils.isNotEmpty(depNodes)) {
                    for (Node depNode : depNodes) {
                        curNode.createRelationshipTo(depNode, relationshipType);
                    }
                }
            }
        }
    }

    private Node getNode(final Map<String, Node> nodeRegister, final BasicBuildPojo buildRelation, final String nodeMark, Label label) {
        Node buildNode;
        if (nodeRegister.containsKey(nodeMark)) {
            buildNode = nodeRegister.get(nodeMark);
        } else {
            buildNode = createNode(buildRelation, label);
            nodeRegister.put(nodeMark, buildNode);
        }
        return buildNode;
    }

    private Map<Node, Integer> generateNodeLongestPath(Label label, RelationshipType relationshipType) {
        final Set<Node> endNodes = queryEndNodes(label, relationshipType);
        final Set<Node> startNodes = queryStartNodes(label, relationshipType);
        Node[] ends = new Node[endNodes.size()];
        int i = 0;
        for (Node endNode : endNodes) {
            ends[i++] = endNode;
        }
        final Transaction tx = graphDb.beginTx();
        final Traverser traverse = graphDb.traversalDescription().depthFirst().uniqueness(Uniqueness.NODE_PATH).relationships(relationshipType, Direction.OUTGOING)/*.evaluator(Evaluators.fromDepth(1))*/.evaluator(Evaluators.pruneWhereEndNodeIs(ends)).traverse(startNodes);
        tx.success();
        Map<Node, Integer> nodeLengthMap = Maps.newHashMap();
        for (Path path : traverse) {
            System.out.println(path);
            final Iterable<Node> nodes = path.nodes();
            int k = 0;
            for (Node node : nodes) {
                if (nodeLengthMap.containsKey(node)) {
                    final Integer nodeLength = nodeLengthMap.get(node);
                    if (nodeLength >= k) {
                        k++;
                        continue;
                    }
                }
                nodeLengthMap.put(node, k++);
            }
        }
        return nodeLengthMap;
    }

    private List<Set<Node>> convert2buildSort(final Map<Node, Integer> nodeIntegerMap) {

        Map<Integer, Set<Node>> lengthNodeMap = Maps.newHashMap();
        for (Map.Entry<Node, Integer> entry : nodeIntegerMap.entrySet()) {
            final Integer length = entry.getValue();
            final Node node = entry.getKey();
            if (lengthNodeMap.containsKey(length)) {
                lengthNodeMap.get(length).add(node);
            } else {
                lengthNodeMap.put(length, Sets.newHashSet(node));
            }
        }
        List<Map.Entry<Integer, Set<Node>>> lengthNodeMapList = Lists.newArrayList(lengthNodeMap.entrySet());
        Collections.sort(lengthNodeMapList, new Comparator<Map.Entry<Integer, Set<Node>>>() {
            @Override
            public int compare(final Map.Entry<Integer, Set<Node>> m1, final Map.Entry<Integer, Set<Node>> m2) {
                return m1.getKey() < m2.getKey() ? 1 : -1;
            }
        });
        List<Set<Node>> nodeBuildList = Lists.newArrayList();
        for (Map.Entry<Integer, Set<Node>> entry : lengthNodeMapList) {
            nodeBuildList.add(entry.getValue());
        }
        return nodeBuildList;
    }


    private Set<Node> queryStartNodes(Label label, RelationshipType relationshipType) {
        return getStartOrEndNodes(true, label, relationshipType);
    }

    private Set<Node> queryEndNodes(Label label, RelationshipType relationshipType) {
        return getStartOrEndNodes(false, label, relationshipType);

    }

    private Set<Node> getStartOrEndNodes(boolean isStartNode, Label label, RelationshipType relationshipType) {
        Set<Node> outcomeNodes = Sets.newHashSet();
        try (Transaction tx = graphDb.beginTx()) {
            final ResourceIterator<Node> buildNodes = graphDb.findNodes(label);
            while (buildNodes.hasNext()) {
                final Node buildNode = buildNodes.next();
                if (buildNode.hasRelationship(relationshipType)) {
                    final Iterable<Relationship> jarRelationships = buildNode.getRelationships(relationshipType);
                    boolean isOutcome = true;
                    for (Relationship jarRelationship : jarRelationships) {
                        final Node node = isStartNode ? jarRelationship.getEndNode() : jarRelationship.getStartNode();
                        if (node.equals(buildNode)) {
                            isOutcome = false;
                            break;
                        }
                    }
                    if (isOutcome) {
                        outcomeNodes.add(buildNode);
                    }
                } else {
                    outcomeNodes.add(buildNode);
                }
            }
            tx.success();
        }
        return outcomeNodes;
    }

    private Node createNode(final BasicBuildPojo buildPojo, Label label) {
        final Node buildNode = graphDb.createNode(label);
        buildNode.setProperty(GROUP_ID, buildPojo.getGroupId());
        buildNode.setProperty(ARTIFACT_ID, buildPojo.getArtifactId());
        buildNode.setProperty(BUILD_PATH, buildPojo.getBuildPath());
        return buildNode;
    }

    // shutdownHook
    private void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

    /*private void registerSuperJar(final String superPomFilePath) throws ParsePomException {
        try {
            final Document document = SAX_READER.read(new File(superPomFilePath));
            final Element rootElement = document.getRootElement();
            final Element propertiesEle = rootElement.element("properties");
            final Iterator<Element> properties = propertiesEle.elementIterator();
            Map<String, String> jarVersionMap = Maps.newHashMap();
            while (properties.hasNext()) {
                final Element versionEle = properties.next();
                jarVersionMap.put("${" + versionEle.getName() + "}", versionEle.getTextTrim());

            }
            final Element dependencyManagementEle = rootElement.element("dependencyManagement");
            final Element dependenciesEle = dependencyManagementEle.element("dependencies");
            final Iterator<Element> dependencyIterator = dependenciesEle.elementIterator("dependency");
            while (dependencyIterator.hasNext()) {
                final Element dependencyEle = dependencyIterator.next();
                final String groupId = dependencyEle.elementTextTrim(GROUP_ID);
                final String artifactId = dependencyEle.elementTextTrim(ARTIFACT_ID);
                final String version = dependencyEle.elementTextTrim("version");
                final String type = dependencyEle.elementTextTrim("type");
                if (!(StringUtils.isNotBlank(type) && "pom".equalsIgnoreCase(type))) {
                    superJarRegister.put(groupId + ":" + artifactId, new BasicBuildPojo(groupId, artifactId, jarVersionMap.getOrDefault(version, version)));
                }
            }
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
    }*/


}
