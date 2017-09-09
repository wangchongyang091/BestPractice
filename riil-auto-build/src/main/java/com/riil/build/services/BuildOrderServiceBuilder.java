package com.riil.build.services;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public enum BuildOrderServiceBuilder {
    INSTANCE;

    public GenJarBuildOrderService newGenJarBuildOrderService() {
        return new GenJarBuildOrderServiceImpl();
    }
}
