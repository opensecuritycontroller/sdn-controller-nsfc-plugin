     package org.osc.controller.nsfc;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osc.controller.nsfc.NeutronSfcSdnRedirectionApiTestData.*;
import static org.osc.sdk.controller.FailurePolicyType.NA;
import static org.osc.sdk.controller.TagEncapsulationType.VLAN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.StringStartsWith;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.osc.controller.nsfc.api.NeutronSfcSdnRedirectionApi;
import org.osc.controller.nsfc.entities.InspectionHookEntity;
import org.osc.controller.nsfc.entities.InspectionPortEntity;
import org.osc.controller.nsfc.entities.NetworkElementEntity;
import org.osc.controller.nsfc.entities.PortPairGroupEntity;
import org.osc.controller.nsfc.entities.ServiceFunctionChainEntity;
import org.osc.controller.nsfc.utils.RedirectionApiUtils;
import org.osc.sdk.controller.DefaultNetworkPort;
import org.osc.sdk.controller.element.Element;
import org.osc.sdk.controller.element.InspectionHookElement;
import org.osc.sdk.controller.element.InspectionPortElement;
import org.osc.sdk.controller.element.NetworkElement;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class NeutronSfcSdnRedirectionApiTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TestTransactionControl txControl;

    private EntityManager em;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    NeutronSfcSdnRedirectionApi redirApi;

    @Before
    public void setup() {
        this.em = InMemDB.getEntityManager();
        this.txControl.init(this.em);
        setupDataObjects();
    }

    @After
    public void tearDown() throws Exception {
        InMemDB.close();
    }

    @Test
    public void testDb_PersistInspectionPort_verifyCorrectNumberOfMacsAdPortIps() throws Exception {

        persistInspectionPort();

        InspectionPortEntity tmp = this.txControl.requiresNew(() -> {
            return this.em.find(InspectionPortEntity.class, inspectionPort.getElementId());
        });

        assertEquals(2, tmp.getEgressPort().getMacAddresses().size());
        assertEquals(2, tmp.getEgressPort().getPortIPs().size());
        assertEquals(2, tmp.getIngressPort().getMacAddresses().size());
        assertEquals(2, tmp.getIngressPort().getPortIPs().size());
    }

    @Test
    public void testUtilsInspectionPortByNetworkElements() throws Exception {

        persistInspectionPort();

        RedirectionApiUtils utils = new RedirectionApiUtils(this.em, this.txControl);

        InspectionPortEntity foundPort = utils.findInspectionPortByNetworkElements(ingress, egress);

        assertNotNull(foundPort);
        assertEquals(inspectionPort.getElementId(), foundPort.getElementId());
    }

    @Test
    public void testUtilsInspHookByInspectedAndPort() throws Exception {
        persistInspectionHook();

        RedirectionApiUtils utils = new RedirectionApiUtils(this.em, this.txControl);

        InspectionHookEntity foundIH = this.txControl.required(() -> {
            ServiceFunctionChainEntity tmpSfc = this.em.find(ServiceFunctionChainEntity.class,
                    sfc.getElementId());

            InspectionHookEntity ihe = utils.findInspHookByInspectedAndPort(inspected, tmpSfc);

            assertNotNull(ihe);
            assertEquals(inspectionHook.getHookId(), ihe.getHookId());
            return ihe;
        });

        assertEquals(foundIH.getHookId(), inspectionHook.getHookId());
        assertEquals(foundIH.getServiceFunctionChain().getElementId(), sfc.getElementId());
        assertEquals(foundIH.getInspectedPort().getElementId(), inspected.getElementId());

    }

    @Test
    public void testUtilsRemoveSingleInspectionHook() throws Exception {
        persistInspectionHook();

        RedirectionApiUtils utils = new RedirectionApiUtils(this.em, this.txControl);

        utils.removeSingleInspectionHook(inspectionHook.getHookId());

        InspectionHookEntity inspectionHookEntity = this.txControl.required(() -> {
            InspectionHookEntity tmpInspectionHook = this.em.find(InspectionHookEntity.class, inspectionHook.getHookId());
            return tmpInspectionHook;
        });

        assertNull(inspectionHookEntity);
    }

    // Inspection port tests

    @Test
    public void testApiRegisterInspectionPort() throws Exception {
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        InspectionPortElement inspectionPortElement = new InspectionPortEntity(null, null, ingress, egress);
        inspectionPortElement = (InspectionPortElement) this.redirApi.registerInspectionPort(inspectionPortElement);

        // Here we are mostly afraid of LazyInitializationException
        assertNotNull(inspectionPortElement.getElementId());
        assertNotNull(inspectionPortElement.getParentId());
        assertNotNull(inspectionPortElement.getIngressPort());
        assertNotNull(inspectionPortElement.getEgressPort());
        assertNotNull(inspectionPortElement.getEgressPort().getMacAddresses());
        assertNotNull(inspectionPortElement.getEgressPort().getElementId());
        assertNotNull(inspectionPortElement.getIngressPort());
        assertNotNull(inspectionPortElement.getIngressPort().getMacAddresses());
        assertNotNull(inspectionPortElement.getIngressPort().getElementId());
        inspectionPortElement.getIngressPort().getParentId();
        inspectionPortElement.getEgressPort().getParentId();

        final InspectionPortElement inspectionPortElementTmp = inspectionPortElement;
        NetworkElementEntity foundIngress = this.txControl.required(() -> {
            return this.em.find(NetworkElementEntity.class, inspectionPortElementTmp.getIngressPort().getElementId());
        });

        assertNotNull(foundIngress);
        assertEquals(inspectionPortElement.getIngressPort().getElementId(), foundIngress.getElementId());

        // Here we are afraid of lazyInitializationException
        foundIngress.getMacAddresses();
        foundIngress.getPortIPs();
        foundIngress.getElementId();
        foundIngress.getParentId();

        InspectionPortElement foundInspPortElement = this.redirApi.getInspectionPort(inspectionPortElement);
        assertEquals(inspectionPortElement.getIngressPort().getElementId(),
                foundInspPortElement.getIngressPort().getElementId());
        assertEquals(inspectionPortElement.getEgressPort().getElementId(),
                foundInspPortElement.getEgressPort().getElementId());
        assertEquals(inspectionPortElement.getElementId(), foundInspPortElement.getElementId());

        assertEquals(foundInspPortElement.getParentId(), inspectionPortElement.getParentId());
    }

    @Test
    public void testApiRegisterInspectionPortWithNetworkElementsAlreadyPersisted() throws Exception {
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.txControl.required(() -> {
            this.em.persist(ingress);
            this.em.persist(egress);
            return null;
        });

        InspectionPortElement inspectionPortElement = new InspectionPortEntity(null, null, ingress, egress);

        // ... and the test
        inspectionPortElement = (InspectionPortElement) this.redirApi.registerInspectionPort(inspectionPortElement);
        assertNotNull(inspectionPortElement);
        assertNotNull(inspectionPortElement.getElementId());
        assertNotNull(inspectionPortElement.getParentId());
        assertNotNull(inspectionPortElement.getIngressPort());
        assertNotNull(inspectionPortElement.getEgressPort());
        assertEquals(ingress.getElementId(), inspectionPortElement.getIngressPort().getElementId());
        assertEquals(egress.getElementId(), inspectionPortElement.getEgressPort().getElementId());
    }

    @Test
    public void testApiRegisterInspectionPortWithParentId() throws Exception {
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        InspectionPortElement inspectionPortElement = new InspectionPortEntity(null, null, ingress, egress);
        Element result = this.redirApi.registerInspectionPort(inspectionPortElement);

        assertNotNull(result.getParentId());
        String portGroupId = result.getParentId();

        RedirectionApiUtils utils = new RedirectionApiUtils(this.em, this.txControl);

        PortPairGroupEntity ppg = utils.findByPortPairgroupId(portGroupId);
        InspectionPortElement inspectionPortElement2 = new InspectionPortEntity(null, ppg,
                new NetworkElementEntity("IngressFoo", asList("IngressMac"), asList("IngressIP"), null),
                new NetworkElementEntity("EgressFoo", asList("EgressMac"), asList("EgressIP"), null));

        Element result2 = this.redirApi.registerInspectionPort(inspectionPortElement2);

        assertEquals(portGroupId, result2.getParentId());
    }

    @Test
    public void testApiRegisterInspectionPortWithInvalidParentId() throws Exception {
        this.exception.expect(IllegalArgumentException.class);

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        PortPairGroupEntity ppg = new PortPairGroupEntity();
        ppg.setElementId("fooportgroup");

        InspectionPortElement inspectionPortElement = new InspectionPortEntity(null, ppg, ingress,
                egress);
        this.redirApi.registerInspectionPort(inspectionPortElement);
    }

    @Test
    public void testApi_RemoveSingleInspectionPort_VerifyPPGDeleted() throws Exception {
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        InspectionPortEntity inspectionPortElement = new InspectionPortEntity(null, null, ingress, egress);

        Element registeredElement = this.redirApi.registerInspectionPort(inspectionPortElement);

        assertTrue(registeredElement instanceof InspectionPortEntity);
        String elementId = registeredElement.getElementId();

        InspectionPortEntity foundInspectionPort = this.txControl.required(() -> {
            InspectionPortEntity tmpInspectionPort = this.em.find(InspectionPortEntity.class, elementId);
            assertNotNull(tmpInspectionPort);
            return tmpInspectionPort;
        });

        assertEquals(elementId, foundInspectionPort.getElementId());
        String ppgId = foundInspectionPort.getParentId();
        String ingressPortId = foundInspectionPort.getIngressPort().getElementId();
        String egressPortId = foundInspectionPort.getEgressPort().getElementId();

        this.redirApi.removeInspectionPort(inspectionPortElement);

        this.txControl.required(() -> {

            assertNull(this.em.find(InspectionPortEntity.class, elementId));
            assertNull(this.em.find(PortPairGroupEntity.class, ppgId));
            assertNull(this.em.find(NetworkElementEntity.class, ingressPortId));
            assertNull(this.em.find(NetworkElementEntity.class, egressPortId));

            return null;
        });

    }

    @Test
    public void testApi_RemoveSingleInspectionPort_VerifyPPGNotDeleted() throws Exception {
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        InspectionPortEntity inspectionPortElement = new InspectionPortEntity(null, null, ingress, egress);

        Element registeredElement = this.redirApi.registerInspectionPort(inspectionPortElement);

        assertTrue(registeredElement instanceof InspectionPortEntity);

        String elementId = registeredElement.getElementId();

        InspectionPortEntity foundInspectionPort = this.txControl.required(() -> {
            InspectionPortEntity tmpInspectionPort = this.em.find(InspectionPortEntity.class, elementId);
            assertNotNull(tmpInspectionPort);
            return tmpInspectionPort;
        });

        assertEquals(elementId, foundInspectionPort.getElementId());

        InspectionPortElement inspectionPortElement2 = new InspectionPortEntity(null,
                foundInspectionPort.getPortPairGroup(),
                new NetworkElementEntity("IngressFoo", asList("IngressMac"), asList("IngressIP"), null),
                new NetworkElementEntity("EgressFoo", asList("EgressMac"), asList("EgressIP"), null));

        registeredElement = this.redirApi.registerInspectionPort(inspectionPortElement2);

        String ppgId = foundInspectionPort.getParentId();

        this.redirApi.removeInspectionPort(inspectionPortElement);

        foundInspectionPort = this.txControl.required(() -> {
            return this.em.find(InspectionPortEntity.class, elementId);
        });

        PortPairGroupEntity ppg = this.txControl.required(() -> {
            return this.em.find(PortPairGroupEntity.class, ppgId);
        });

        assertNull(foundInspectionPort);
        assertNotNull(ppg);
    }

    // Inspection hooks test

    @Test
    public void testApiInstallInspectionHook_VerifySucceeds() throws Exception {
        persistInspectionPort();
        this.txControl.required(() -> {

            sfc.getPortPairGroups().add(ppg);
            this.em.persist(sfc);

            return null;
        });

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        final String hookId = this.redirApi.installInspectionHook(inspected, sfc, 0L, VLAN, 0L,
                NA);

        assertNotNull(hookId);

        InspectionHookElement inspectionHookElement = this.txControl.required(() -> {
            InspectionHookEntity tmp = this.em.find(InspectionHookEntity.class, hookId);
            assertNotNull(tmp);
            assertEquals(tmp.getServiceFunctionChain().getElementId(), sfc.getElementId());
            return tmp;
        });

        // Here we are mostly afraid of LazyInitializationException
        assertNotNull(inspectionHookElement);
        assertNotNull(inspectionHookElement.getHookId());
        assertEquals(inspectionHookElement.getInspectedPort().getElementId(), inspected.getElementId());
    }

    @Test
    public void testApiInstallInspectionHook_WithNoInspectedPort_VerifyFails() throws Exception {

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage("null passed for Inspection port !");

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.redirApi.installInspectionHook(inspected, sfc, 0L, VLAN, 0L,
                NA);

        // Inspected port with non-existing id
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(StringStartsWith.startsWith("Cannot find type Service Function Chain"));

        this.redirApi.installInspectionHook(inspected, new ServiceFunctionChainEntity("foo"), 0L, VLAN, 0L,
                NA);
    }

    @Test
    public void testApiInstallInspectionHook_WithExistingHook_VerifyFails() throws Exception {
        persistInspectionPort();
        this.txControl.required(() -> {

            sfc.getPortPairGroups().add(ppg);
            this.em.persist(sfc);

            return null;
        });

        this.exception.expect(IllegalStateException.class);
        this.exception.expectMessage(StringStartsWith.startsWith("Found existing inspection hook"));

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.redirApi.installInspectionHook(inspected, sfc, 0L, VLAN, 0L, NA);

        this.redirApi.installInspectionHook(inspected, sfc, 0L, VLAN, 0L, NA);
    }

    @Test
    public void testApiUpdateInspectionHook_WithExistingHook_VerifySucceeds() throws Exception {
        persistInspectionHook();

        String hookId = inspectionHook.getHookId();

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Setup new SFC
        InspectionPortElement inspectionPort = new InspectionPortEntity(null,
                null,
                new NetworkElementEntity("IngressFoo", asList("IngressMac"), asList("IngressIP"), null),
                new NetworkElementEntity("EgressFoo", asList("EgressMac"), asList("EgressIP"), null));

        Element portPairEntity = this.redirApi.registerInspectionPort(inspectionPort);

        ServiceFunctionChainEntity newSfc = this.txControl.required(() -> {
            ServiceFunctionChainEntity tmpSfc = new ServiceFunctionChainEntity();
            tmpSfc.getPortPairGroups().add(new PortPairGroupEntity(portPairEntity.getParentId()));
            this.em.persist(tmpSfc);
            return tmpSfc;
        });

        InspectionHookEntity updatedHook = new InspectionHookEntity(inspected, newSfc);
        updatedHook.setHookId(hookId);

        // Act
        this.redirApi.updateInspectionHook(updatedHook);

        this.txControl.required(() -> {
            InspectionHookEntity tmp = this.em.find(InspectionHookEntity.class, hookId);
            assertNotNull(tmp);
            assertEquals(tmp.getServiceFunctionChain().getElementId(), newSfc.getElementId());
            return null;
        });
    }

    @Test
    public void testApiUpdateInspectionHook_WithMissingHook_VerifyFailure() throws Exception {
        persistInspectionPortAndSfc();

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        InspectionHookEntity updatedHook = new InspectionHookEntity(inspected, sfc);
        updatedHook.setHookId("non-existing-id");

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(StringStartsWith.startsWith("Cannot find Inspection Hook"));

        // Act
        this.redirApi.updateInspectionHook(updatedHook);
    }

    @Test
    public void testApiRemoveInspectionHookById_InspectionHookDisappears() throws Exception {
        persistInspectionHook();

        InspectionHookEntity inspectionHookEntity = this.txControl.required(() -> {
            InspectionHookEntity tmpInspectionHook = this.em.find(InspectionHookEntity.class, inspectionHook.getHookId());
            return tmpInspectionHook;
        });

        assertNotNull(inspectionHookEntity);

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);
        this.redirApi.removeInspectionHook(inspectionHookEntity.getHookId());

        inspectionHookEntity = this.txControl.required(() -> {
            InspectionHookEntity tmpInspectionHook = this.em.find(InspectionHookEntity.class, inspectionHook.getHookId());
            return tmpInspectionHook;
        });

        NetworkElementEntity inspectedPortNetworkElement = this.txControl.required(() -> {
            return this.em.find(NetworkElementEntity.class, inspected.getElementId());
        });

        assertNull(inspectionHookEntity);
        assertNull(inspectedPortNetworkElement);
    }

    @Test
    public void testApiGetInspectionHook() throws Exception {
        persistInspectionHook();

        InspectionHookElement inspectionHookElement = this.txControl.required(() -> {
            InspectionHookEntity tmp = this.em.find(InspectionHookEntity.class, inspectionHook.getHookId());
            assertNotNull(tmp);
            assertEquals(tmp.getServiceFunctionChain().getElementId(), sfc.getElementId());
            return tmp;
        });

        // Here we are mostly afraid of LazyInitializationException
        assertNotNull(inspectionHookElement);
        assertNotNull(inspectionHookElement.getHookId());
        assertEquals(inspectionHookElement.getInspectedPort().getElementId(), inspected.getElementId());
    }

    @Test
    public void testRegisterNetworkElementWithNullPPGList_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Port Pair Group member list"));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        this.redirApi.registerNetworkElement(null);
    }

    @Test
    public void testRegisterNetworkElementWithEmptyPPGList_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        List<NetworkElement> neList = new ArrayList<NetworkElement>();

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Port Pair Group member list"));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        this.redirApi.registerNetworkElement(neList);
    }

    @Test
    public void testRegisterNetworkElementWithPpgIdNull_ThrowsIllegalArgumentException() throws Exception {
        // Arrange

        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        neList.add(ne);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !",  "Port Pair Group Id"));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        this.redirApi.registerNetworkElement(neList);
    }

    @Test
    public void testRegisterNetworkElementWithInvalidPpgId_ThrowsIllegalArgumentException() throws Exception {
        // Arrange

        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        ne.setElementId("badId");
        neList.add(ne);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("Cannot find %s by id: %s!", "Port Pair Group", ne.getElementId()));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);


        // Act
        this.redirApi.registerNetworkElement(neList);
    }

    @Test
    public void testRegisterNetworkElementWithPpgIdIsChainedToAnotherSfc_ThrowsIllegalArgumentException()
            throws Exception {
        // Arrange
        persistInspectionPortAndSfc();
        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        ne.setElementId(ppg.getElementId());
        neList.add(ne);

        this.exception.expect(IllegalArgumentException.class);
        this.exception
                .expectMessage(String.format(String.format("Port Pair Group Id %s is already chained to SFC Id : %s ",
                        ne.getElementId(), ppg.getServiceFunctionChain().getElementId())));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        this.redirApi.registerNetworkElement(neList);
    }

    @Test
    public void testRegisterNetworkElement_VerifySuccess() throws Exception {
        // Arrange
        persistInspectionPort();
        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        ne.setElementId(ppg.getElementId());
        neList.add(ne);

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);


        // Act
        NetworkElement neResponse=  this.redirApi.registerNetworkElement(neList);
        this.txControl.required(() -> {
            ServiceFunctionChainEntity sfc = this.em.find(ServiceFunctionChainEntity.class, neResponse.getElementId());
            assertNotNull("SFC is not to be found after creation", sfc);
            return null;
        });
    }

    @Test
    public void testUpdateNetworkElementWithNullSfc_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(
                String.format(String.format("null passed for %s !", "Port Pair Group Service Function Chain Id")));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        this.redirApi.updateNetworkElement(null, neList);
    }

    @Test
    public void testUpdateNetworkElementWithSfcIdNull_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Port Pair Group Service Function Chain Id"));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        this.redirApi.updateNetworkElement(ne, neList);
    }

    @Test
    public void testUpdateNetworkElementWithNullUpdatedPpgList_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Port Pair Group update member list"));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);
        ne.setElementId("goodid");

        // Act
        this.redirApi.updateNetworkElement(ne, null);
    }

    @Test
    public void testUpdateNetworkElementWithEmptyUpdatedPpgList_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Port Pair Group update member list"));
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);
        ne.setElementId("goodid");

        // Act
        this.redirApi.updateNetworkElement(ne, neList);
    }

    @Test
    public void testUpdateNetworkElementWhenSfcToUpdateIsNotFound_ThrowsIllegalArgumentException() throws Exception {
        // Arrange

        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        ne.setElementId("bad-id");
        neList.add(ne);

        this.exception.expect(IllegalArgumentException.class);
        this.exception
                .expectMessage(String.format("Cannot find %s by id: %s!", "Service Function Chain", ne.getElementId()));

        // Act
        this.redirApi.updateNetworkElement(ne, neList);
    }

    @Test
    public void testUpdateNetworkElementWhenPpgIdIsNullInUpdatedList_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        persistInspectionPortAndSfc();

        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort sfc = new DefaultNetworkPort();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        sfc.setElementId(sfc.getElementId());

        ne.setParentId("BadId");
        neList.add(ne);

        this.exception.expect(IllegalArgumentException.class);

        this.exception.expectMessage(new BaseMatcher<String>() {

            @Override
            public boolean matches(Object s) {
                if (!(s instanceof String)) {
                    return false;
                }
                return ((String)s).matches(".*null.+Port Pair Group Service Function Chain Id.*");
            }

            @Override
            public void describeTo(Description description) {
                // TODO Auto-generated method stub

            }});

        // Act
        this.redirApi.updateNetworkElement(sfc, neList);
    }

    @Test
    public void testUpdateNetworkElementWhenPpgIdInUpdatedListIsNotFound_ThrowsIllegalArgumentException()
            throws Exception {
        // Arrange
        persistInspectionPortAndSfc();

        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort sfcTest = new DefaultNetworkPort();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        sfcTest.setElementId(sfc.getElementId());

        ne.setElementId("BadPpgId");
        ne.setParentId(sfc.getElementId());
        neList.add(ne);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("Cannot find %s by id: %s!", "Port Pair Group", ne.getElementId()));

        // Act
        this.redirApi.updateNetworkElement(sfcTest, neList);
    }

    @Test
    public void testUpdateNetworkElementWhenPpgIdIsChainedToSameSfc_VerifySuccessful()
            throws Exception {
        // Arrange
        persistInspectionPortAndSfc();

        List<NetworkElement> neList = new ArrayList<NetworkElement>();
        DefaultNetworkPort ne = new DefaultNetworkPort();
        DefaultNetworkPort sfcTest = new DefaultNetworkPort();

        sfcTest.setElementId(sfc.getElementId());

        ne.setElementId(ppg.getElementId());
        neList.add(ne);

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        // Act
        NetworkElement sfcReturn = this.redirApi.updateNetworkElement(sfcTest, neList);
        Assert.assertEquals("Return Sfc is not equal tosfc", sfc.getElementId(), sfcReturn.getElementId());
    }

    @Test
    public void testUpdateNetworkElement_VerifySuccessful() throws Exception {
        // Arrange
        List<PortPairGroupEntity> ppgList = persistNInspectionPort(4);
        ServiceFunctionChainEntity sfcPersist = persistNppgsInSfc(ppgList);

        List<NetworkElement> neReverseList = new ArrayList<NetworkElement>();
        DefaultNetworkPort sfcTest = new DefaultNetworkPort();

        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        sfcTest.setElementId(sfcPersist.getElementId());

        Collections.reverse(ppgList);
        List<String> ppgListSrc = new ArrayList<String>();
        for(PortPairGroupEntity ppg_local : ppgList) {
            DefaultNetworkPort ne = new DefaultNetworkPort();
            ne.setElementId(ppg_local.getElementId());
            ne.setParentId(sfcPersist.getElementId());
            neReverseList.add(ne);
            ppgListSrc.add(ppg_local.getElementId());
        }

        // Act
        NetworkElement neResponse = this.redirApi.updateNetworkElement(sfcTest, neReverseList);
        this.txControl.required(() -> {
            ServiceFunctionChainEntity sfcTarget = this.em.find(ServiceFunctionChainEntity.class, neResponse.getElementId());
            assertNotNull("SFC is not to be found after creation", sfcTarget);
            List<String> ppgListTarget = new ArrayList<String>();
            for(PortPairGroupEntity ppg_local : sfcTarget.getPortPairGroups()) {
                ppgListTarget.add(ppg_local.getElementId());
            }
            Assert.assertEquals("The list of port pair group ids is different than expected", ppgListSrc, ppgListTarget);
            return null;
        });
    }

    @Test
    public void testDeleteNetworkElementWhenSfcToDeleteIsNotFound_ThrowsIllegalArgumentException() throws Exception {
        // Arrange

        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        ne.setElementId("bad-id");

        this.exception.expect(IllegalArgumentException.class);
        this.exception
                .expectMessage(String.format("Cannot find %s by id: %s!", "Service Function Chain", ne.getElementId()));

        // Act
        this.redirApi.deleteNetworkElement(ne);
    }

    @Test
    public void testDeleteNetworkElementWhenSfcElementIsNull_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Service Function Chain Id"));

        // Act
        this.redirApi.deleteNetworkElement(null);
    }

    @Test
    public void testDeleteNetworkElementWhenSfcIdIsNull_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Service Function Chain Id"));

        // Act
        this.redirApi.deleteNetworkElement(ne);
    }

    @Test
    public void testDeleteNetworkElement_VerifySuccessful() throws Exception {
        // Arrange
        persistInspectionPortAndSfc();
        String localSfcId = sfc.getElementId();

        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        ne.setElementId(sfc.getElementId());

        // Act
        this.redirApi.deleteNetworkElement(ne);
        this.txControl.required(() -> {
            ServiceFunctionChainEntity sfc_t = this.em.find(ServiceFunctionChainEntity.class, localSfcId);
            assertNull("SFC still exist after deletion", sfc_t);
            return null;
        });
    }

    @Test
    public void testGetNetworkElementWhenSfcElementIsNull_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Service Function Chain Id"));

        // Act
        this.redirApi.getNetworkElements(null);
    }

    @Test
    public void testGetNetworkElementWhenSfcIdIsNull_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("null passed for %s !", "Service Function Chain Id"));

        // Act
        this.redirApi.getNetworkElements(ne);
    }

    @Test
    public void testGetNetworkElementWhenSfcGetIsNotFound_ThrowsIllegalArgumentException() throws Exception {
        // Arrange

        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        ne.setElementId("bad-id");

        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage(String.format("Cannot find %s by id: %s!", "Service Function Chain", ne.getElementId()));

        // Act
        this.redirApi.getNetworkElements(ne);
    }

    @Test
    public void testGetNetworkElement_VerifySuccessful() throws Exception {
        // Arrange
        persistInspectionPortAndSfc();

        DefaultNetworkPort ne = new DefaultNetworkPort();
        this.redirApi = new NeutronSfcSdnRedirectionApi(this.txControl, this.em);

        ne.setElementId(sfc.getElementId());

        // Act
        List<NetworkElement> neResponseList = this.redirApi.getNetworkElements(ne);
        assertNotNull("SFC chain List is Empty", neResponseList);
    }

    private InspectionPortEntity persistInspectionPort() {
        return this.txControl.required(() -> {
            this.em.persist(ppg);

            inspectionPort.setPortPairGroup(ppg);
            this.em.persist(inspectionPort);

            ppg.getPortPairs().add(inspectionPort);

            this.em.merge(ppg);
            return inspectionPort;
        });
    }


    private List<PortPairGroupEntity> persistNInspectionPort(int count) {
        List<PortPairGroupEntity> ppgList = new ArrayList<PortPairGroupEntity>();
        for(int i=0;i<count;i++) {
            InspectionPortEntity insp = new InspectionPortEntity();
            PortPairGroupEntity ppg_n= new PortPairGroupEntity();

            this.txControl.required(() -> {
                    this.em.persist(ppg_n);

                    insp.setPortPairGroup(ppg_n);
                    this.em.persist(insp);

                    ppg_n.getPortPairs().add(insp);
                    this.em.merge(ppg_n);
                    return null;
            });
            ppgList.add(ppg_n);
        }
        return ppgList;
    }

    private ServiceFunctionChainEntity persistNppgsInSfc(List<PortPairGroupEntity> ppgList) {

        return this.txControl.required(() -> {
            for(PortPairGroupEntity ppg : ppgList) {
                sfc.getPortPairGroups().add(ppg);
                this.em.persist(sfc);

                ppg.setServiceFunctionChain(sfc);
                this.em.merge(ppg);
            }

            return sfc;
        });
    }

    private ServiceFunctionChainEntity persistInspectionPortAndSfc() {
        persistInspectionPort();
        return this.txControl.required(() -> {
            sfc.getPortPairGroups().add(ppg);
            this.em.persist(sfc);

            ppg.setServiceFunctionChain(sfc);
            this.em.merge(ppg);
            return sfc;
        });
    }

    private InspectionHookEntity persistInspectionHook() {
        persistInspectionPort();
        return this.txControl.required(() -> {
            sfc.getPortPairGroups().add(ppg);
            this.em.persist(sfc);

            ppg.setServiceFunctionChain(sfc);
            this.em.merge(ppg);

            this.em.persist(inspected);

            this.em.persist(inspectionHook);
            return inspectionHook;
        });
    }

}
