package com.riil.build.services;

import com.google.common.collect.Maps;
import com.riil.build.exception.GenDependencyRealtionException;
import com.riil.build.exception.ParsePomException;
import com.riil.build.pojo.BasicBuildPojo;
import com.riil.build.pojo.BasicBuildRelationPojo;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public interface GenJarBuildOrderService {
    Map<String, BasicBuildPojo> superJarRegister = Maps.newHashMap();

    void registerSuperRiilJar(String superRiilPomPath) throws ParsePomException;

    void registerSuper3rdJar(String super3rdPomPath) throws ParsePomException;

    List<File> getPomsBySpecifyRange(String path) throws ParsePomException;

    Map<String, String> genProjectBuildPathRegister(List<File> pomFiles) throws ParsePomException;

    Set<BasicBuildRelationPojo> getBuildRelations(List<File> pomFiles) throws ParsePomException;

    List<Set<BasicBuildPojo>> getJarBuildOrder(Set<BasicBuildRelationPojo> jarDirectPojos, String neo4jDbPath) throws GenDependencyRealtionException;
}
