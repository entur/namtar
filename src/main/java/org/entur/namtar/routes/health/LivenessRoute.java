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

package org.entur.namtar.routes.health;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.entur.namtar.netex.NetexLoader;
import org.entur.namtar.routes.RestRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class LivenessRoute extends RestRouteBuilder {

    @Autowired
    private NetexLoader netexLoader;

    @Value("${namtar.health.allowed.inactivity.hours:48}")
    private int allowedInactivityHours;
    private int allowedInactivitySeconds;

    @Override
    public void configure() throws Exception {
        super.configure();

        allowedInactivitySeconds = allowedInactivityHours*60*60;

        rest("/health")
            .apiDocs(Boolean.FALSE)
            .get("/ready").to("direct:health.ready")
            .get("/up").to("direct:health.up")
        ;

        from("direct:health.ready")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("200"))
                .setBody(constant("OK"))
                .routeId("health.ready");

        from("direct:health.up")
                .choice()
                .when(p -> netexLoader.getLastSuccessfulDataLoaded().isBefore(Instant.now().minusSeconds(allowedInactivitySeconds)))
                    .log(LoggingLevel.ERROR, "Data not processed in " + allowedInactivityHours + " hours. Triggering restart")
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("503")) // Service unavailable
                    .setBody(constant("Data not processed during the last " + allowedInactivityHours + " hours"))
                .otherwise()
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("200"))
                    .setBody(constant("OK"))
                .endChoice()
                .routeId("health.up");
    }
}
