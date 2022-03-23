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

package org.entur.namtar.routes.data;

import org.apache.camel.LoggingLevel;
import org.entur.namtar.netex.NetexLoader;
import org.entur.namtar.routes.RestRouteBuilder;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BlobStoreRoute extends RestRouteBuilder {

    private final NetexLoader netexLoader;

    @Value("${namtar.blobstore.polling.update.frequency}")
    private String updateFrequency;

    @Value("${namtar.import.disabled:false}")
    boolean importDisabled;

    public BlobStoreRoute(@Autowired NetexLoader netexLoader) {
        this.netexLoader = netexLoader;
    }

    @Override
    public void configure() {
        getContext().setUseBreadcrumb(true);

        log.info("Polling for updates with frequency: [{}]", updateFrequency);

        singletonFrom("timer://namtar.blobstore.polling?fixedRate=true&period=" + updateFrequency,
                "namtar.blobstore.singleton.polling")
                .process(p -> MDC.put("camel.breadcrumbId", p.getIn().getHeader("breadcrumbId", String.class)))
                .choice()
                .when(p -> importDisabled)
                    .log(LoggingLevel.WARN, "Import disabled - doing nothing")
                .endChoice()
                .when(p -> isLeader(p.getFromRouteId()))
                    .log("Is leader - polling for new files")
                    .to("direct:getAllBlobs")
                    .wireTap("direct:loadBlobs")
                .endChoice()
                .otherwise()
                    .log("Is NOT leader - doing nothing")
                .end()
                .process(p -> MDC.remove("camel.breadcrumbId"))
        ;

        // TODO: If successful, delete/move file from bucket


        final String logParams = "?level=DEBUG&showAll=true&multiline=true";

        from("direct:getAllBlobs")
                .to("log:" + getClass().getName() + logParams)
                .bean("blobStoreService", "getAllBlobs")
                .to("log:" + getClass().getName() + logParams)
                .routeId("blobstore-list")
        ;

        from("direct:loadBlobs")
                .process(p -> MDC.put("camel.breadcrumbId", p.getIn().getHeader("breadcrumbId", String.class)))
                .to("log:" + getClass().getName() + logParams)
                .bean(netexLoader, "loadNetexFromBlobStore")
                .to("log:" + getClass().getName() + logParams)
                .process(p -> MDC.remove("camel.breadcrumbId"))
                .routeId("blobstore-load")
        ;
    }
}
