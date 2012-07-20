/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.client;

import javax.inject.Inject;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.ProviderBinder;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.base.Function;

/**
 * Function that can be put to an acceptor chain to properly initialize
 * the client-side request-scoped processing injection for the current
 * request and response exchange.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RequestProcessingInitializationStage implements Function<ClientRequest, ClientRequest> {

    private static final class References {
        @Inject
        private Ref<ClientConfig> configuration;
        @Inject
        private Ref<ExceptionMappers> exceptionMappers;
        @Inject
        private Ref<MessageBodyWorkers> messageBodyWorkers;
        @Inject
        private Ref<ContextResolvers> contextResolvers;
        @Inject
        Ref<ClientRequest> requestContextRef;
    }

    private final ServiceLocator locator;
    private final ProviderBinder providerBinder;

    /**
     * Create new {@link org.glassfish.jersey.message.MessageBodyWorkers} initialization function
     * for requests and responses.
     *
     * @param locator       HK2 locator.
     * @param providerBinder Jersey provider binder.
     */
    @Inject
    public RequestProcessingInitializationStage(
            ServiceLocator locator,
            ProviderBinder providerBinder) {
        this.locator = locator;
        this.providerBinder = providerBinder;
    }


    @Override
    public ClientRequest apply(ClientRequest requestContext) {
        References refs = locator.createAndInitialize(References.class); // request-scoped

        final ClientConfig cfg = requestContext.getConfiguration();
        providerBinder.bindClasses(cfg.getProviderClasses());
        providerBinder.bindInstances(cfg.getProviderInstances());
        final ExceptionMapperFactory mappers = new ExceptionMapperFactory(locator);
        final MessageBodyWorkers workers = new MessageBodyFactory(locator);
        final ContextResolvers resolvers = new ContextResolverFactory(locator);

        refs.configuration.set(cfg);

        refs.exceptionMappers.set(mappers);
        refs.messageBodyWorkers.set(workers);
        refs.contextResolvers.set(resolvers);
        refs.requestContextRef.set(requestContext);

        requestContext.setWorkers(workers);

        return requestContext;
    }
}