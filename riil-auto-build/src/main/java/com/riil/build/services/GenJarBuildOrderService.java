package com.riil.build.services;

import com.google.common.collect.Maps;
import com.riil.build.exception.GenDependencyRealtionException;
import com.riil.build.exception.ParsePomException;
import com.riil.build.pojo.BasicBuildPojo;
import com.riil.build.pojo.BasicBuildRelationPojo;
import com.riil.build.pojo.BuildParams;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public interface GenJarBuildOrderService {
    Map<String, BasicBuildPojo> superJarRegister = Maps.newHashMap();
    Map<String, Set<String>> aggregatePomRegister = Maps.newHashMap();

    @Deprecated
    void registerSuperRiilJar(String superRiilPomPath) throws ParsePomException;

    @Deprecated
    void registerSuper3rdJar(String super3rdPomPath) throws ParsePomException;

    List<File> getPomsBySpecifyRange(File specifyPath) throws ParsePomException;

    Map<String, Set<String>> aggregatePomRegister(List<File> pomFiles) throws ParsePomException;

    Map<String, String> genProjectBuildPathRegister(List<File> pomFiles) throws ParsePomException;

    Set<BasicBuildRelationPojo> genBuildRelations(List<File> pomFiles) throws ParsePomException;

    void addData2Graph(BuildParams buildParams);

    List<Set<BasicBuildPojo>> getJarBuildOrder(BuildParams buildParams) throws GenDependencyRealtionException;

    enum MyLabels implements Label {
        BASICBUILD, JAR
    }

    enum MyRelationshipTypes implements RelationshipType {
        AFTER, DEPEND_ON
    }
}
