/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.storage.infinispan;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.Config;

public class CacheInitializer {

    private final String config;

    public CacheInitializer(String config) {
        this.config = config;
    }

    public DefaultCacheManager getCacheManager(Config.Scope config) {
        try {
            ConfigurationBuilderHolder builder = new ParserRegistry().parse(this.config);

            if (builder.getNamedConfigurationBuilders().get("sessions").clustering().cacheMode().isClustered()) {
                configureTransportStack(config, builder);
            }

            // For Infinispan 10, we go with the JBoss marshalling.
            // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
            // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
            builder.getGlobalConfigurationBuilder().serialization().marshaller(new JBossUserMarshaller());

            return new DefaultCacheManager(builder, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void configureTransportStack(Config.Scope config, ConfigurationBuilderHolder builder) {
        String transportStack = config.get("stack");

        if (transportStack != null) {
            builder.getGlobalConfigurationBuilder().transport().defaultTransport()
                    .addProperty("configurationFile", "default-configs/default-jgroups-" + transportStack + ".xml");
        }
    }
}
