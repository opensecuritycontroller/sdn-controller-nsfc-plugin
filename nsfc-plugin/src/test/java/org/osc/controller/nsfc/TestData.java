/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.controller.nsfc;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.openstack4j.api.Builders;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.api.networking.ext.FlowClassifierService;
import org.openstack4j.api.networking.ext.PortChainService;
import org.openstack4j.api.networking.ext.PortPairGroupService;
import org.openstack4j.api.networking.ext.PortPairService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.ext.FlowClassifier;
import org.openstack4j.model.network.ext.PortChain;
import org.openstack4j.model.network.ext.PortPair;
import org.openstack4j.model.network.ext.PortPairGroup;
import org.openstack4j.model.network.options.PortListOptions;
import org.osc.controller.nsfc.entities.FlowClassifierElement;
import org.osc.controller.nsfc.entities.NetworkElementImpl;
import org.osc.controller.nsfc.entities.PortPairElement;
import org.osc.controller.nsfc.entities.PortPairGroupElement;
import org.osc.controller.nsfc.entities.ServiceFunctionChainElement;

class TestData {

    private static final String EADDR1_STR = "192.168.0.11";

    private static final String IADDR1_STR = "10.4.3.1";

    private static final String EMAC1_STR = "ee:ff:aa:bb:cc:01";

    private static final String IMAC1_STR = "ff:ff:aa:bb:cc:01";

    private static final String INSPMAC1_STR = "aa:aa:aa:bb:cc:01";

    private static final String INSPADDR_STR = "10.4.5.6";
    
    private static final String GATEWAYIFACEADDR1_STR = "10.0.0.1";
    
    private static final String GATEWAYIFACEMACADDR1_STR = "aa:aa:aa:bb:cc:01";

    private static final Random ID_GENERATOR = new Random(System.currentTimeMillis());

    public static FlowClassifierElement inspectionHook;
    public static PortPairElement inspectionPort;
    public static PortPairGroupElement ppgElement;
    public static ServiceFunctionChainElement sfc;

    public static NetworkElementImpl ingressPortElement;
    public static NetworkElementImpl egressPortElement;
    public static NetworkElementImpl inspectedPortElement;
    public static NetworkElementImpl defaultGatewayPortElement;
    
    public static PortChain portChain;
    public static PortPair portPair;
    public static PortPairGroup portPairGroup;
    public static Port ingressPort;
    public static Port egressPort;
    public static Port inspectedPort;
    public static Port defaultGatewayPort;

    public static PortService portService;
    public static PortChainService portChainService;
    public static PortPairService portPairService;
    public static PortPairGroupService portPairGroupService;
    public static FlowClassifierService flowClassifierService;

    public static void setupDataObjects() {
        ingressPortElement = new NetworkElementImpl();
        ingressPortElement.setElementId(IMAC1_STR + IMAC1_STR);
        ingressPortElement.setMacAddresses(asList(IMAC1_STR));
        ingressPortElement.setPortIPs(asList(IADDR1_STR));

        egressPortElement = new NetworkElementImpl();
        egressPortElement.setElementId(EMAC1_STR + EMAC1_STR);
        egressPortElement.setMacAddresses(asList(EMAC1_STR));
        egressPortElement.setPortIPs(asList(EADDR1_STR));

        inspectedPortElement = new NetworkElementImpl();
        inspectedPortElement.setElementId("iNsPeCtEdPoRt");
        inspectedPortElement.setMacAddresses(asList(INSPMAC1_STR));
        inspectedPortElement.setPortIPs(asList(INSPADDR_STR));
        
        defaultGatewayPortElement = new NetworkElementImpl();
        defaultGatewayPortElement.setElementId("defaultgateway");
        defaultGatewayPortElement.setMacAddresses(asList(GATEWAYIFACEMACADDR1_STR));
        defaultGatewayPortElement.setPortIPs(asList(GATEWAYIFACEADDR1_STR));
        
        ppgElement = new PortPairGroupElement();

        inspectionPort = new PortPairElement();
        inspectionPort.setIngressPort(ingressPortElement);
        inspectionPort.setEgressPort(egressPortElement);

        sfc = new ServiceFunctionChainElement();

        inspectionHook = new FlowClassifierElement(null, inspectedPortElement, sfc);

        portChain = Builders.portChain().build();
        portPair = Builders.portPair().build();
        portPairGroup = Builders.portPairGroup().build();

        portService = new MockPortService();
        portChainService = new MockPortChainService();
        portPairService = new MockPortPairService();
        portPairGroupService = new MockPortPairGroupService();
        flowClassifierService = new MockFlowClassifierService();
    }

    private static class CRUDMockService<T extends org.openstack4j.model.common.Resource> {
        Map<String, T> dataObjects = new HashMap<String, T>();

        public List<? extends T> list()  {
            return new ArrayList<T>(this.dataObjects.values());
        }

        public T get(String id) {
            if (id == null) {
                throw new IllegalArgumentException("id cannot be null");
            }
            return this.dataObjects.get(id);
        }

        public T create(T object) {
            String id;
            do {
                id = ID_GENERATOR.nextLong() + "";
            } while (this.dataObjects.keySet().contains(id));

            object.setId(id);
            this.dataObjects.put(id, object);
            return object;
        }

        public T update(String id, T object) {
            if (id == null) {
                throw new IllegalArgumentException("id cannot be null");
            }

            object.setId(id);
            this.dataObjects.put(id, object);

            return object;
        }

        public ActionResponse delete(String id) {
            if (id == null) {
                throw new IllegalArgumentException("id cannot be null");
            }
            this.dataObjects.remove(id);
            return ActionResponse.actionSuccess();
        }
    }

    private static class MockPortService extends CRUDMockService<Port> implements PortService {

        @Override
        public List<? extends Port> list(PortListOptions options) {
            return list();
        }

        @Override
        public List<? extends Port> create(List<? extends Port> ports) {
            if (ports == null) {
                throw new IllegalArgumentException("List of ports cannot be null");
            }
            return ports.stream().map(p -> create(p)).collect(toList());
        }

        @Override
        public Port update(Port object) {
            return update(object.getId(), object);
        }

    }
    private static class MockPortChainService extends CRUDMockService<PortChain> implements PortChainService {
    }
    private static class MockPortPairGroupService extends CRUDMockService<PortPairGroup> implements PortPairGroupService {
    }
    private static class MockPortPairService extends CRUDMockService<PortPair> implements PortPairService {
    }
    private static class MockFlowClassifierService extends CRUDMockService<FlowClassifier> implements FlowClassifierService {
    }

    static PortPairService mockPortPairService() {
        return new MockPortPairService();
    }
}
