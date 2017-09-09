package com.riil.build.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.riil.build.exception.GenDependencyRealtionException;
import com.riil.build.exception.ParsePomException;
import com.riil.build.pojo.JarDirectPojo;
import com.riil.build.pojo.JarPojo;
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
    public static final SAXReader SAX_READER = new SAXReader();
    private GraphDatabaseService graphDb;
    private String localRepository = "D:\\m2_repository";

    @Override
    public void register3rdJar(final String super3rdPom) throws ParsePomException {
        try {
            final Document document = SAX_READER.read(new File(super3rdPom));
            final Element rootElement = document.getRootElement();
            final Element propertiesEle = rootElement.element("properties");
            final Iterator<Element> properties = propertiesEle.elementIterator();
            Map<String, String> jarVersionMap = Maps.newHashMap();
            while (properties.hasNext()) {
                final Element versionEle = properties.next();
                jarVersionMap.put(versionEle.getName(), versionEle.getTextTrim());

            }
            System.out.println(jarVersionMap);
        } catch (Exception e) {
            throw new ParsePomException(e);
        }

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

    @Override
    public JarDirectPojo getDirectDependencyByPom(final String pomFilePath) throws ParsePomException {
        final JarDirectPojo jarDirectPojo = new JarDirectPojo();
        try {
            final File pomFile = new File(pomFilePath);
            final Document document = SAX_READER.read(pomFile);
            final Element rootElement = document.getRootElement();
            jarDirectPojo.setGroupId(rootElement.elementTextTrim("groupId"));
            jarDirectPojo.setArtifactId(rootElement.elementTextTrim("artifactId"));
            final String version = rootElement.element("parent").elementTextTrim("version");
            jarDirectPojo.setVersion(version);
            final Element dependencies = rootElement.element("dependencies");
            final Iterator dependencyEle = dependencies.elementIterator("dependency");
            Set<JarPojo> jarDirectPojoSet = Sets.newHashSet();
            while (dependencyEle.hasNext()) {
                final Element dependency = (Element) dependencyEle.next();
                final JarPojo jarPojo = new JarPojo();
                jarPojo.setGroupId(dependency.elementTextTrim("groupId"));
                jarPojo.setArtifactId(dependency.elementTextTrim("artifactId"));
                jarDirectPojoSet.add(jarPojo);
            }
            jarDirectPojo.setDirectDependency(jarDirectPojoSet);
        } catch (Exception e) {
            throw new ParsePomException(e);
        }
        return jarDirectPojo;
    }

    @Override
    public List<Set<JarPojo>> getJarBuildOrder(final List<JarDirectPojo> jarDirectPojos, String neo4jDbPath) throws GenDependencyRealtionException {
        try {
            startDb(neo4jDbPath);
            createDb(jarDirectPojos);
            final Map<Node, Integer> nodeIntegerMap = generateNodeLongestPath();
            final List<Set<Node>> buildNodeList = convert2buildSort(nodeIntegerMap);
            return covert2JarList(buildNodeList);
        } catch (Exception e) {
            throw new GenDependencyRealtionException(e);
        }
    }

    private List<Set<JarPojo>> covert2JarList(final List<Set<Node>> buildNodeList) {
        List<Set<JarPojo>> buildJarList = Lists.newArrayList();
        for (Set<Node> nodes : buildNodeList) {
            Set<JarPojo> jarPojoSet = Sets.newHashSet();
            for (Node node : nodes) {
                final JarPojo jarPojo = new JarPojo();
                jarPojo.setGroupId((String) node.getProperty("groupId"));
                jarPojo.setArtifactId((String) node.getProperty("artifactId"));
                jarPojo.setVersion((String) node.getProperty("version"));
                jarPojoSet.add(jarPojo);
            }
            buildJarList.add(jarPojoSet);
        }
        return buildJarList;
    }

    private void startDb(String neo4jDbPath) {
        //        startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(neo4jDbPath));
        registerShutdownHook(graphDb);
    }

    private void createDb(List<JarDirectPojo> jarDirectPojos) {
//        FileUtils.deleteRecursively(DB_PATH);
//        transaction
        try (Transaction tx = graphDb.beginTx()) {
//            addData
            addData(jarDirectPojos);
            tx.success();
        }
    }

    private void addData(List<JarDirectPojo> jarDirectPojos) {
        if (CollectionUtils.isNotEmpty(jarDirectPojos)) {
            Map<Node, Set<Node>> nodeRelationMap = Maps.newHashMap();
            Map<String, Node> nodeRegister = Maps.newHashMap();
            for (JarDirectPojo jarDirectPojo : jarDirectPojos) {
                String nodeMark = String.format("%s:%s:%s", jarDirectPojo.getGroupId(), jarDirectPojo.getArtifactId(), jarDirectPojo.getVersion());
                Node jarNode = null;
                if (nodeRegister.containsKey(nodeMark)) {
                    jarNode = nodeRegister.get(nodeMark);
                } else {
                    jarNode = createNode(jarDirectPojo);
                    nodeRegister.put(nodeMark, jarNode);
                }
                final Set<JarPojo> jarDepSet = jarDirectPojo.getDirectDependency();
                Set<Node> jarDepNodeSet = Sets.newHashSet();
                if (CollectionUtils.isNotEmpty(jarDepSet)) {
                    for (JarPojo jarPojo : jarDepSet) {
                        nodeMark = String.format("%s:%s:%s", jarPojo.getGroupId(), jarPojo.getArtifactId(), jarPojo.getVersion());
                        Node jarDepNode;
                        if (nodeRegister.containsKey(nodeMark)) {
                            jarDepNode = nodeRegister.get(nodeMark);
                        } else {
                            jarDepNode = createNode(jarPojo);
                            nodeRegister.put(nodeMark, jarDepNode);
                        }
                        jarDepNodeSet.add(jarDepNode);
                    }
                }
                nodeRelationMap.put(jarNode, jarDepNodeSet);
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

    private Node createNode(final JarPojo jarDirectPojo) {
        final Node jarNode = graphDb.createNode(MyLabels.JAR);
        jarNode.setProperty("groupId", jarDirectPojo.getGroupId());
        jarNode.setProperty("artifactId", jarDirectPojo.getArtifactId());
        jarNode.setProperty("version", jarDirectPojo.getVersion());
        return jarNode;
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

    enum MyLabels implements Label {
        JAR
    }

    enum MyRelationshipTypes implements RelationshipType {
        DEPEND_ON
    }
}
