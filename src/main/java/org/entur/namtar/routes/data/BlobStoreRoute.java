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

import org.entur.namtar.netex.NetexLoader;
import org.entur.namtar.routes.RestRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BlobStoreRoute extends RestRouteBuilder {

    private final NetexLoader netexLoader;

    @Value("${namtar.blobstore.polling.update.frequency}")
    private String updateFrequency;

    public BlobStoreRoute(@Autowired NetexLoader netexLoader) {
        this.netexLoader = netexLoader;
    }

    @Override
    public void configure() {

        log.info("Polling for updates with frequency: [{}]", updateFrequency);

        singletonFrom("timer://namtar.blobstore.polling?fixedRate=true&period=" + updateFrequency,
                "namtar.blobstore.singleton.polling")
                .choice()
                .when(p -> isLeader(p.getFromRouteId()))
                    .log("Is leader - polling for new files")
                    .to("direct:getAllBlobs")
                    .wireTap("direct:loadBlobs")
                .endChoice()
                .otherwise()
                    .log("Is NOT leader - doing nothing")
                .end()
        ;

        // TODO: If successful, delete/move file from bucket


        from("direct:getAllBlobs")
                .to("log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true")
                .bean("blobStoreService", "getAllBlobs")
                .to("log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true")
                .routeId("blobstore-list")
        ;

        from("direct:loadBlobs")
                .to("log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true")
                .bean(netexLoader, "loadNetexFromBlobStore")
                .to("log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true")
                .routeId("blobstore-load")
        ;
    }
}
