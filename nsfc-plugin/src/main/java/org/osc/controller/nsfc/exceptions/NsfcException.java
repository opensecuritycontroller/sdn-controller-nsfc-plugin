package org.osc.controller.nsfc.exceptions;

import org.openstack4j.model.common.Resource;

public abstract class NsfcException extends RuntimeException {
    private static final long serialVersionUID = 1774920883105069044L;

    protected final Class<? extends Resource> objectClass;
    protected final Operation operation;

    public enum Operation {Create, Update};

    public NsfcException(Operation operation, Class<? extends Resource> objectClass) {
        this.objectClass = objectClass;
        this.operation = operation;
    }

    public NsfcException(Operation operation, Class<? extends Resource> objectClass, Throwable cause) {
        super(cause);
        this.objectClass = objectClass;
        this.operation = operation;
    }

    @Override
    public String getMessage() {
        return String.format("%s %s failed!", this.operation, this.objectClass);
    }
}
