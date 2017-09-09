package com.riil.build.exception;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class GenDependencyRealtionException extends AutoBuildException {
    public GenDependencyRealtionException() {
        super();
    }

    public GenDependencyRealtionException(final String s) {
        super(s);
    }

    public GenDependencyRealtionException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public GenDependencyRealtionException(final Throwable throwable) {
        super(throwable);
    }
}
