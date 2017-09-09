package com.riil.build.services;

import com.google.common.collect.Maps;
import com.riil.build.exception.GenDependencyRealtionException;
import com.riil.build.exception.ParsePomException;
import com.riil.build.pojo.JarDirectPojo;
import com.riil.build.pojo.JarPojo;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public interface GenJarBuildOrderService {
    Map<String, JarPojo> super3rdJarRegister = Maps.newHashMap();

    void register3rdJar(String super3rdPom) throws ParsePomException;

    List<File> getPomsBySpecifyRange(String path) throws ParsePomException;

    JarDirectPojo getDirectDependencyByPom(String pomFilePath) throws ParsePomException;

    List<Set<JarPojo>> getJarBuildOrder(List<JarDirectPojo> jarDirectPojos, String neo4jDbPath) throws GenDependencyRealtionException;
}
