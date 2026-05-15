/*******************************************************************************
 * Copyright (c) 2023, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.network.status.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.network.status.provider.api.FailureDTO;
import org.eclipse.kura.network.status.provider.api.InterfaceIdsDTO;
import org.eclipse.kura.network.status.provider.api.InterfaceStatusListDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/networkStatus/v1")
@Component(name = "org.eclipse.kura.internal.network.status.provider.NetworkStatusRestServiceImpl", //
    immediate = true, //
    property = { //
        "service.pid=org.eclipse.kura.internal.network.status.provider.NetworkStatusRestServiceImpl", //
        "kura.service.pid=org.eclipse.kura.internal.network.status.provider.NetworkStatusRestServiceImpl", //
        "osgi.jakartars.resource=true" //
    }, //
    service = NetworkStatusRestServiceImpl.class //
)
public class NetworkStatusRestServiceImpl {

    private static final String KURA_PERMISSION_REST_NETWORK_STATUS_ROLE = "kura.permission.rest.network.status";

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatusRestServiceImpl.class);
    private static final String APP_ID = "NET-STATUS-V1";

    private NetworkStatusService networkStatusService;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "unsetRequestHandlerRegistry")
    public void setRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("failed to register request handler", e);
        }
    }

    public void unsetRequestHandlerRegistry(final RequestHandlerRegistry registry) {
        try {
            registry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.warn("failed to unregister request handler", e);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setNetworkStatusService(final NetworkStatusService networkStatusService) {
        this.networkStatusService = networkStatusService;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_NETWORK_STATUS_ROLE, Role.GROUP);
    }

    @GET
    @RolesAllowed("network.status")
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public InterfaceStatusListDTO getNetworkStatus() {
        try {
            return getInterfaceStatusInternal(this.networkStatusService.getInterfaceIds());
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @POST
    @RolesAllowed("network.status")
    @Path("/status/byInterfaceId")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public InterfaceStatusListDTO getNetworkStatus(final InterfaceIdsDTO interfaceIds) {
        try {
            interfaceIds.validate();

            return getInterfaceStatusInternal(interfaceIds.getInterfaceIds());
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    @GET
    @RolesAllowed("network.status")
    @Path("/interfaceIds")
    @Produces(MediaType.APPLICATION_JSON)
    public InterfaceIdsDTO getInterfaceNames() {
        try {
            return new InterfaceIdsDTO(this.networkStatusService.getInterfaceIds());
        } catch (final Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
    }

    private InterfaceStatusListDTO getInterfaceStatusInternal(final List<String> interfaceIds) {
        final List<NetworkInterfaceStatus> interfaces = new ArrayList<>();
        final List<FailureDTO> failures = new ArrayList<>();

        for (final String interfaceId : interfaceIds) {
            try {
                final NetworkInterfaceStatus status = this.networkStatusService.getNetworkStatus(interfaceId)
                        .orElseThrow(() -> new KuraException(KuraErrorCode.NOT_FOUND, "Interface not found"));

                interfaces.add(status);
            } catch (final Exception e) {
                failures.add(new FailureDTO(interfaceId, e));
            }
        }

        return new InterfaceStatusListDTO(interfaces, failures);
    }

}
