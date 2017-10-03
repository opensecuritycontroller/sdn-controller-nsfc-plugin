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
package org.osc.controller.nsfc.entities;

import static javax.persistence.FetchType.EAGER;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.osc.sdk.controller.element.InspectionPortElement;

@Entity
@Table(name = "INSPECTION_PORT")
public class InspectionPortEntity implements InspectionPortElement {

    //Port pair id
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "element_id", unique = true)
    private String elementId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "port_pair_group_fk", nullable = false,
            foreignKey = @ForeignKey(name = "FK_INSPECTION_PORT_PPG"))
    private PortPairGroupEntity portPairGroup;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = false, fetch = EAGER, optional = true)
    @JoinColumn(name = "ingress_fk",
            nullable = true,
            updatable = true,
            foreignKey = @ForeignKey(name = "FK_INSPECTION_PORT_NETWORK_ELEMENT_INGR"))
    private NetworkElementEntity ingressPort;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = false, fetch = EAGER, optional = true)
    @JoinColumn(name = "egress_fk", nullable = true, updatable = true,
            foreignKey = @ForeignKey(name = "FK_INSPECTION_PORT_NETWORK_ELEMENT_EGR"))
    private NetworkElementEntity egressPort;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = false, fetch = EAGER, mappedBy="inspectionPort")
    private Set<InspectionHookEntity> inspectionHooks = new HashSet<>();

    public InspectionPortEntity() {
    }

    public InspectionPortEntity(String elementId, PortPairGroupEntity portPairGroup, NetworkElementEntity ingress, NetworkElementEntity egress) {
        this.elementId = elementId;
        this.portPairGroup = portPairGroup;
        this.ingressPort = ingress;
        this.egressPort = egress;
        this.inspectionHooks = new HashSet<>();
    }

    @Override
    public String getElementId() {
        return this.elementId;
    }

    @Override
    public NetworkElementEntity getIngressPort() {
        return this.ingressPort;
    }

    public void setIngressPort(NetworkElementEntity ingressPort) {
        this.ingressPort = ingressPort;
    }

    @Override
    public NetworkElementEntity getEgressPort() {
        return this.egressPort;
    }

    public void setEgressPort(NetworkElementEntity egressPort) {
        this.egressPort = egressPort;
    }

    public Set<InspectionHookEntity> getInspectionHooks() {
        return this.inspectionHooks;
    }

    public void setInspectionHooks(Collection<InspectionHookEntity> inspectionHooks) {
        this.inspectionHooks = new HashSet<>(inspectionHooks);
    }

    public PortPairGroupEntity getPortPairGroup() {
        return this.portPairGroup;
    }

    public void setPortPairGroup(PortPairGroupEntity portPairGroup) {
        this.portPairGroup = portPairGroup;
    }

    @Override
    public String getParentId() {
        return this.portPairGroup == null ? null : this.portPairGroup.getElementId();
    }

    @Override
    public String toString() {
        // use get elementid on ppg to avoid cyclic dependency and stackoverflow issues
        return "InspectionPortEntity [elementId=" + this.elementId + ", portPairGroup=" + getParentId()
                + ", ingressPort=" + this.ingressPort + ", egressPort=" + this.egressPort + ", inspectionHooks="
                + this.inspectionHooks + "]";
    }

}
