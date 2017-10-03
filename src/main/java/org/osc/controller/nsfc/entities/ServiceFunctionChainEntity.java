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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "SERVICE_FUNCTION_CHAIN")
public class ServiceFunctionChainEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "element_id", unique = true)
    private String elementId;

    @OneToMany(mappedBy = "serviceFunctionChain", fetch = FetchType.EAGER)
    @OrderColumn(name = "ppg_order")
    private List<PortPairGroupEntity> portPairGroups = new ArrayList<>();

    public ServiceFunctionChainEntity() {
    }

    public ServiceFunctionChainEntity(String elementId, String parentId) {
        super();
        this.elementId = elementId;
    }

    public String getElementId() {
        return this.elementId;
    }

    public List<PortPairGroupEntity> getPortPairGroups() {
        return this.portPairGroups;
    }

    public void setPortPairGroups(List<PortPairGroupEntity> portPairGroups) {
        this.portPairGroups = portPairGroups;
    }

    @Override
    public String toString() {
        return "ServiceFunctionChainEntity [elementId=" + this.elementId + ", portPairGroups=" + this.portPairGroups + "]";
    }

}
