import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

/**
 * User: wangchongyang on 2017/8/30 0030.
 */
public class EmbeddedNeo4j {

    private static final File DB_PATH = new File("E:\\works\\neo4j\\neo4j.db");
    private GraphDatabaseService graphDb;

    public static void main(String[] args) throws IOException {
        final long start = System.currentTimeMillis();
        final EmbeddedNeo4j embeddedNeo4j = new EmbeddedNeo4j();
        embeddedNeo4j.startDb();
//        final Set<Node> startNodes = embeddedNeo4j.queryStartNodes();
//        System.out.println(startNodes);
//        final Set<Node> endNodes = embeddedNeo4j.queryEndNodes();
//        System.out.println(endNodes);
        final Map<Node, Integer> nodeIntegerMap = embeddedNeo4j.generateNodeLongestPath();
        convert2buildSort(nodeIntegerMap);
//        embeddedNeo4j.generateNodeLongestPath();
//        embeddedNeo4j.createDb();
//        embeddedNeo4j.removeData();
        System.out.println("cost " + (System.currentTimeMillis() - start) + "ms");
        embeddedNeo4j.shutDown();
        System.out.println("shutdown cost " + (System.currentTimeMillis() - start) + "ms");
    }

    private static void convert2buildSort(final Map<Node, Integer> nodeIntegerMap) {

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
    }

    // shutdownHook
    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
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

    private Set<Node> queryStartNodes() {
        return getStartOrEndNodes(true);
    }

    private Set<Node> queryEndNodes() {
        return getStartOrEndNodes(false);

    }

    private Set<Node> getStartOrEndNodes(boolean isStartNode) {
        Set<Node> endNodes = Sets.newHashSet();
        try (Transaction tx = graphDb.beginTx()) {
            final ResourceIterator<Node> jarNodes = graphDb.findNodes(RootEnum.MyLabels.JAR);
            while (jarNodes.hasNext()) {
                final Node jarNode = jarNodes.next();
                if (jarNode.hasRelationship(RootEnum.MyRelationshipTypes.DEPEND_ON)) {
                    final Iterable<Relationship> jarRelationships = jarNode.getRelationships(RootEnum.MyRelationshipTypes.DEPEND_ON);
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

    private Map<Node, Integer> generateNodeLongestPath() {
        final Set<Node> endNodes = queryEndNodes();
        final Set<Node> startNodes = queryStartNodes();
        Node[] ends = new Node[10];
        int i = 0;
        for (Node endNode : endNodes) {
            ends[i++] = endNode;
        }
        final Transaction tx = graphDb.beginTx();
        final Traverser traverse = graphDb.traversalDescription().depthFirst().uniqueness(Uniqueness.NODE_PATH).relationships(RootEnum.MyRelationshipTypes.DEPEND_ON, Direction.OUTGOING).evaluator(Evaluators.fromDepth(1)).evaluator(Evaluators.pruneWhereEndNodeIs(ends)).traverse(startNodes);
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

    private void removeData() {
        try (Transaction tx = graphDb.beginTx()) {
//            removingData
            final ResourceIterable<Node> allNodes = graphDb.getAllNodes();
            for (Node node : allNodes) {
                if (node.hasLabel(RootEnum.MyLabels.JAR)) {
                    System.out.println(node.getProperty("artifactId"));
                    final Iterable<Relationship> relationships = node.getRelationships(RootEnum.MyRelationshipTypes.DEPEND_ON, Direction.BOTH);
                    for (Relationship relationship : relationships) {
                        relationship.delete();
                    }
                    node.delete();
                    tx.success();
                    System.out.println("delete success!");
                }
            }
        }
    }

    private void createDb() throws IOException {
//        FileUtils.deleteRecursively(DB_PATH);

//        transaction
        try (Transaction tx = graphDb.beginTx()) {
//            addData
            addMockData();
//            addJarData(graphDbSvc);
            tx.success();
        }
    }

    private void addMockData() {
        final Node nodeA = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeA.setProperty("name", "A");
        final Node nodeB = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeB.setProperty("name", "B");
        final Node nodeC = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeC.setProperty("name", "C");
        final Node nodeD = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeD.setProperty("name", "D");
        final Node nodeE = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeE.setProperty("name", "E");
        final Node nodeF = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeF.setProperty("name", "F");
        final Node nodeG = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeG.setProperty("name", "G");
        final Node nodeH = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeH.setProperty("name", "H");
        final Node nodeI = graphDb.createNode(RootEnum.MyLabels.JAR);
        nodeI.setProperty("name", "I");

        nodeA.createRelationshipTo(nodeB, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeA.createRelationshipTo(nodeG, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeB.createRelationshipTo(nodeC, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeB.createRelationshipTo(nodeF, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeC.createRelationshipTo(nodeD, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeD.createRelationshipTo(nodeE, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeG.createRelationshipTo(nodeD, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeH.createRelationshipTo(nodeC, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeH.createRelationshipTo(nodeG, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeH.createRelationshipTo(nodeI, RootEnum.MyRelationshipTypes.DEPEND_ON);
        nodeI.createRelationshipTo(nodeE, RootEnum.MyRelationshipTypes.DEPEND_ON);
    }

    private void addJarData() {
        final Node bmcFramework = graphDb.createNode();
        bmcFramework.setProperty("groupId", "com.riil.bmc.framework.web");
        bmcFramework.setProperty("artifactId", "riil-bmc-framework");
        bmcFramework.setProperty("version", "6.8.7-SNAPSHOT");
        bmcFramework.addLabel(RootEnum.MyLabels.JAR);
        final Node bmcFrameworkBiz = graphDb.createNode();
        bmcFrameworkBiz.setProperty("groupId", "com.riil.bmc.framework.web");
        bmcFrameworkBiz.setProperty("artifactId", "riil-bmc-framework-biz");
        bmcFrameworkBiz.setProperty("version", "6.8.7-SNAPSHOT");
        bmcFrameworkBiz.addLabel(RootEnum.MyLabels.JAR);
        final Node dataqueryApi = graphDb.createNode();
        dataqueryApi.setProperty("groupId", "com.riil.bmc.framework.business");
        dataqueryApi.setProperty("artifactId", "riil-dataquery-services");
        dataqueryApi.setProperty("version", "6.8.7-SNAPSHOT");
        dataqueryApi.addLabel(RootEnum.MyLabels.JAR);
        final Node dataqueryImpl = graphDb.createNode();
        dataqueryImpl.setProperty("groupId", "com.riil.bmc.framework.business");
        dataqueryImpl.setProperty("artifactId", "riil-dataquery-impl");
        dataqueryImpl.setProperty("version", "6.8.7-SNAPSHOT");
        dataqueryImpl.addLabel(RootEnum.MyLabels.JAR);
        final Node core = graphDb.createNode();
        core.setProperty("groupId", "com.riil.server.framework");
        core.setProperty("artifactId", "riil-core");
        core.setProperty("version", "6.8.7-SNAPSHOT");
        core.addLabel(RootEnum.MyLabels.JAR);
        final Node serverManager = graphDb.createNode();
        serverManager.setProperty("groupId", "com.riil.server.components");
        serverManager.setProperty("artifactId", "riil-server-manager");
        serverManager.setProperty("version", "6.8.7-SNAPSHOT");
        serverManager.addLabel(RootEnum.MyLabels.JAR);
        bmcFramework.createRelationshipTo(core, RootEnum.MyRelationshipTypes.DEPEND_ON);
        bmcFramework.createRelationshipTo(serverManager, RootEnum.MyRelationshipTypes.DEPEND_ON);
        bmcFramework.createRelationshipTo(dataqueryApi, RootEnum.MyRelationshipTypes.DEPEND_ON);
    }

    private void startDb() {
        //        startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        registerShutdownHook(graphDb);
    }

    void shutDown() {
        System.out.println();
        System.out.println("Shutting down database ...");
        // shutdownServer
        graphDb.shutdown();
        // shutdownServer
    }
}
