/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.entur.namtar.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.policy.HazelcastRoutePolicy;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.spi.RoutePolicy;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static org.entur.namtar.routes.policy.SingletonRoutePolicyFactory.SINGLETON_ROUTE_DEFINITION_GROUP_NAME;

public class RestRouteBuilder extends RouteBuilder {


    @Value("${namtar.incoming.port}")
    String incomingPort;

    @Value("${namtar.swagger.host.url}")
    String hostUrl;

    @Override
    public void configure() throws Exception {
        restConfiguration("jetty")
                .port(incomingPort)
                .apiContextPath("api/api-doc")
                    .apiProperty("api.title", "DatedServiceJourneys").apiProperty("api.version", "0.1")
                    // and enable CORS
                    .apiProperty("cors", "true")
                .apiProperty("host", hostUrl)
                .apiProperty("schemes","https")
        ;
    }

    /**
     * Create a new singleton route definition from URI. Only one such route should be active throughout the cluster at any time.
     */
    protected RouteDefinition singletonFrom(String uri, String routeId) {
        return this.from(uri)
                .group(SINGLETON_ROUTE_DEFINITION_GROUP_NAME)
                .routeId(routeId)
                .autoStartup(true);
    }

    protected boolean isLeader(String routeId) {
        RouteContext routeContext = getContext().getRoute(routeId).getRouteContext();
        List<RoutePolicy> routePolicyList = routeContext.getRoutePolicyList();
        if (routePolicyList != null) {
            for (RoutePolicy routePolicy : routePolicyList) {
                if (routePolicy instanceof HazelcastRoutePolicy) {
                    return ((HazelcastRoutePolicy) (routePolicy)).isLeader();
                }
            }
        }
        return false;
    }
}
