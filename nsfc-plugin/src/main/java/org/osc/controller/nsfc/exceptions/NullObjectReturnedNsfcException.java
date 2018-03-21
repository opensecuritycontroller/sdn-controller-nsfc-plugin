package org.osc.controller.nsfc.exceptions;

import org.openstack4j.model.common.Resource;

public class NullObjectReturnedNsfcException extends NsfcException {

    private static final long serialVersionUID = 6875318554247928167L;

    public NullObjectReturnedNsfcException(Operation operation, Class<? extends Resource> objectClass) {
        super(operation, objectClass);
    }

    public NullObjectReturnedNsfcException(Operation operation, Class<? extends Resource> objectClass, Throwable cause) {
        super(operation, objectClass, cause);
    }


    @Override
    public String getMessage() {
        return String.format("%s. SDN Controller returned null %s!", super.getMessage(), this.objectClass);
    }
}
