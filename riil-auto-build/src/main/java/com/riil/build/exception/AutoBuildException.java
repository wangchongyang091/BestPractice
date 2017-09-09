package com.riil.build.exception;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class AutoBuildException extends Exception {
    public AutoBuildException() {
        super();
    }

    public AutoBuildException(final String s) {
        super(s);
    }

    public AutoBuildException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public AutoBuildException(final Throwable throwable) {
        super(throwable);
    }
}
