package org.osc.controller.nsfc.exceptions;

import org.openstack4j.model.common.Resource;

public class SdnControllerResponseNsfcException extends NsfcException {

    private static final long serialVersionUID = -5757616005985528232L;

    public SdnControllerResponseNsfcException(Operation operation, Class<? extends Resource> objectClass) {
        super(operation, objectClass);
    }

    public SdnControllerResponseNsfcException(Operation operation, Class<? extends Resource> objectClass, Throwable cause) {
        super(operation, objectClass, cause);
    }

    @Override
    public String getMessage() {
        return String.format("%s. SDN Controller threw  %s: %s!", super.getMessage(),
                getCause().getClass().getSimpleName(), getCause().getMessage());
    }

}
