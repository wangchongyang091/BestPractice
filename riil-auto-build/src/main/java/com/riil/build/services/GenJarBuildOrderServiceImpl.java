package com.riil.build.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.riil.build.exception.GenDependencyRealtionException;
import com.riil.build.exception.ParsePomException;
import com.riil.build.pojo.BasicBuildPojo;
import com.riil.build.pojo.BasicBuildRelationPojo;
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
    private static final SAXReader SAX_READER = new SAXReader();
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
    public List<File> getPomsBySpecifyRange(final String path) throws ParsePomException {
        if (StringUtils.isEmpty(path)) {
            throw new ParsePomException("Specify parse path should not be null!");
        }
        try {
            final File parseFile = new File(path);
            List<File> pomList = Lists.newArrayList();
            return getPomFiles(pomList, parseFile);
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
                String groupId = projectEle.elementTextTrim("groupId");
                final String artifactId = projectEle.elementTextTrim("artifactId");
                if (StringUtils.isBlank(groupId)) {
                    groupId = projectEle.element("parent").elementTextTrim("groupId");
                }
                buildPathRegister.put(String.format("%s:%s", groupId, artifactId), pomFile.getParent());
            }
            return buildPathRegister;
        } catch (DocumentException e) {
            throw new ParsePomException(e);
        }
    }

    @Override
    public Set<BasicBuildRelationPojo> getBuildRelations(final List<File> pomFiles) throws ParsePomException {
        final Map<String, String> buildPathRegister = genProjectBuildPathRegister(pomFiles);
        Set<BasicBuildRelationPojo> relationPojoSet = Sets.newHashSet();
        try {
            for (File pomFile : pomFiles) {
                final Document document = SAX_READER.read(pomFile);
                final Element projectEle = document.getRootElement();
                final String parentGroupId = projectEle.element("parent").elementTextTrim("groupId");
                final String parentArtifactId = projectEle.element("parent").elementTextTrim("artifactId");
                Set<BasicBuildPojo> jarDirectPojoSet = Sets.newHashSet();
                addBeforeBuildPath(buildPathRegister, jarDirectPojoSet, parentGroupId, parentArtifactId);
                final BasicBuildRelationPojo RelationPojo = new BasicBuildRelationPojo();
                RelationPojo.setBuildPath(pomFile.getParent());
                final Element dependencyManagementEle = projectEle.element("dependencyManagement");
                if (null != dependencyManagementEle) {
                    final Iterator dependencyEles = dependencyManagementEle.element("dependencies").elementIterator("dependency");
                    while (dependencyEles.hasNext()) {
                        final Element dependencyEle = (Element) dependencyEles.next();
                        if ("pom".equals(dependencyEle.elementTextTrim("type"))) {
                            addBeforeBuildPath(buildPathRegister, jarDirectPojoSet, dependencyEle.elementTextTrim("groupId"), dependencyEle.elementTextTrim("artifactId"));
                        }
                    }
                } else {
                    final Iterator dependencyEles = projectEle.element("dependencies").elementIterator("dependency");
                    while (dependencyEles.hasNext()) {
                        final Element dependencyEle = (Element) dependencyEles.next();
                        addBeforeBuildPath(buildPathRegister, jarDirectPojoSet, dependencyEle.elementTextTrim("groupId"), dependencyEle.elementTextTrim("artifactId"));
                    }
                }
                RelationPojo.setBeforeBuilds(jarDirectPojoSet);
                relationPojoSet.add(RelationPojo);
            }
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
        return relationPojoSet;
    }

    @Override
    public List<Set<BasicBuildPojo>> getJarBuildOrder(final Set<BasicBuildRelationPojo> jarDirectPojos, String neo4jDbPath) throws GenDependencyRealtionException {
        try {
            startDb(neo4jDbPath);
//            createDb(jarDirectPojos);
            final Map<Node, Integer> nodeIntegerMap = generateNodeLongestPath();
            final List<Set<Node>> buildNodeList = convert2buildSort(nodeIntegerMap);
            return covert2JarList(buildNodeList);
        } catch (Exception e) {
            throw new GenDependencyRealtionException(e);
        }
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
                basicBuildPojoSet.add(new BasicBuildPojo((String) node.getProperty("buildPath")));
            }
            buildJarList.add(basicBuildPojoSet);
        }
        return buildJarList;
    }


    private void addBeforeBuildPath(final Map<String, String> buildPathRegister, final Set<BasicBuildPojo> jarDirectPojoSet, final String groupId, final String artifactId) {
        String buildPath = buildPathRegister.get(String.format("%s:%s", groupId, artifactId));
        if (null != buildPath) {
            jarDirectPojoSet.add(new BasicBuildPojo(buildPath));
        }
    }

    private void startDb(String neo4jDbPath) {
        //        startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jDbPath));
        registerShutdownHook(graphDb);
    }

    private void createDb(Set<BasicBuildRelationPojo> jarDirectPojos) {
//        FileUtils.deleteRecursively(DB_PATH);
//        transaction
        try (Transaction tx = graphDb.beginTx()) {
//            addData
            addData(jarDirectPojos);
            tx.success();
        }
    }

    private void addData(Set<BasicBuildRelationPojo> jarDirectPojos) {
        if (CollectionUtils.isNotEmpty(jarDirectPojos)) {
            Map<Node, Set<Node>> nodeRelationMap = Maps.newHashMap();
            Map<String, Node> nodeRegister = Maps.newHashMap();
            for (BasicBuildRelationPojo buildRelation : jarDirectPojos) {
                String nodeMark = buildRelation.getBuildPath();
                Node buildNode = getNode(nodeRegister, buildRelation, nodeMark);
                final Set<BasicBuildPojo> jarDepSet = buildRelation.getBeforeBuilds();
                Set<Node> jarDepNodeSet = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(jarDepSet)) {
                    for (BasicBuildPojo build : jarDepSet) {
                        nodeMark = build.getBuildPath();
                        Node jarDepNode = getNode(nodeRegister, build, nodeMark);
                        jarDepNodeSet.add(jarDepNode);
                    }
                }
                nodeRelationMap.put(buildNode, jarDepNodeSet);
            }
            for (Map.Entry<Node, Set<Node>> nodeSetEntry : nodeRelationMap.entrySet()) {
                final Set<Node> depNodes = nodeSetEntry.getValue();
                if (CollectionUtils.isNotEmpty(depNodes)) {
                    final Node curNode = nodeSetEntry.getKey();
                    for (Node depNode : depNodes) {
                        curNode.createRelationshipTo(depNode, MyRelationshipTypes.DEPEND_ON);
                    }
                }
            }
        }
    }

    private Node getNode(final Map<String, Node> nodeRegister, final BasicBuildPojo buildRelation, final String nodeMark) {
        Node buildNode;
        if (nodeRegister.containsKey(nodeMark)) {
            buildNode = nodeRegister.get(nodeMark);
        } else {
            buildNode = createNode(buildRelation);
            nodeRegister.put(nodeMark, buildNode);
        }
        return buildNode;
    }

    private Map<Node, Integer> generateNodeLongestPath() {
        final Set<Node> endNodes = queryEndNodes();
        final Set<Node> startNodes = queryStartNodes();
        Node[] ends = new Node[endNodes.size()];
        int i = 0;
        for (Node endNode : endNodes) {
            ends[i++] = endNode;
        }
        final Transaction tx = graphDb.beginTx();
        final Traverser traverse = graphDb.traversalDescription().depthFirst().uniqueness(Uniqueness.NODE_PATH).relationships(MyRelationshipTypes.DEPEND_ON, Direction.OUTGOING).evaluator(Evaluators.fromDepth(1)).evaluator(Evaluators.pruneWhereEndNodeIs(ends)).traverse(startNodes);
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
        System.out.println(nodeLengthMap);
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
        System.out.println(nodeBuildList);
        return nodeBuildList;
    }


    private Set<Node> queryStartNodes() {
        return getStartOrEndNodes(true);
    }

    private Set<Node> queryEndNodes() {
        return getStartOrEndNodes(false);

    }

    private Set<Node> getStartOrEndNodes(boolean isStartNode) {
        Set<Node> endNodes = Sets.newHashSet();
        try (Transaction tx = graphDb.beginTx()) {
            final ResourceIterator<Node> jarNodes = graphDb.findNodes(MyLabels.JAR);
            while (jarNodes.hasNext()) {
                final Node jarNode = jarNodes.next();
                if (jarNode.hasRelationship(MyRelationshipTypes.DEPEND_ON)) {
                    final Iterable<Relationship> jarRelationships = jarNode.getRelationships(MyRelationshipTypes.DEPEND_ON);
                    boolean isOutcome = true;
                    for (Relationship jarRelationship : jarRelationships) {
                        final Node node = isStartNode ? jarRelationship.getEndNode() : jarRelationship.getStartNode();
                        if (node.equals(jarNode)) {
                            isOutcome = false;
                            break;
                        }
                    }
                    if (isOutcome) {
                        endNodes.add(jarNode);
                    }
                }
            }
            tx.success();
        }
        return endNodes;
    }

    private Node createNode(final BasicBuildPojo jarDirectPojo) {
        final Node buildNode = graphDb.createNode(MyLabels.JAR);
        buildNode.setProperty("buildPath", jarDirectPojo.getBuildPath());
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
                final String groupId = dependencyEle.elementTextTrim("groupId");
                final String artifactId = dependencyEle.elementTextTrim("artifactId");
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

    enum MyLabels implements Label {
        JAR
    }

    enum MyRelationshipTypes implements RelationshipType {
        DEPEND_ON
    }
}
