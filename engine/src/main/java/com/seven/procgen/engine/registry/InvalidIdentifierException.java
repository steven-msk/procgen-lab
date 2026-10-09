package com.seven.procgen.engine.registry;

public class InvalidIdentifierException extends RuntimeException {
    public InvalidIdentifierException(Identifier id) {
        this(id.getPath());
    }

    public InvalidIdentifierException(String path) {
        super("Invalid identifier: " + path);
    }
}
