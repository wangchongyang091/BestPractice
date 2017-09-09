package com.riil.build.exception;

/**
 * User: wangchongyang on 2017/9/8 0008.
 */
public class ParsePomException extends AutoBuildException {
    public ParsePomException() {
        super();
    }

    public ParsePomException(final String s) {
        super(s);
    }

    public ParsePomException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public ParsePomException(final Throwable throwable) {
        super(throwable);
    }
}
