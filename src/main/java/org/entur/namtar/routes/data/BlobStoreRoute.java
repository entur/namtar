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

import org.apache.camel.builder.RouteBuilder;
import org.entur.namtar.netex.NetexLoader;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BlobStoreRoute extends RouteBuilder {

    private final NetexLoader netexLoader;

    @Value("${namtar.blobstore.polling.cron.expression}")
    private String cronExpression;

    public BlobStoreRoute(@Autowired NetexLoader netexLoader) {
        this.netexLoader = netexLoader;
    }

    @Override
    public void configure() throws Exception {

        log.info("Check with cron-expression [{}], first upload at: {}.", cronExpression,
                new CronExpression(cronExpression).getNextValidTimeAfter(new Date()));


        // TODO: Use singleton route to support multiple instances
        from("quartz2://namtar.blobstore.polling?cron=" + cronExpression)
                .to("direct:getAllBlobs")
                .bean(netexLoader, "loadNetexFromBlobStore")
                .routeId("namtar-blobstore.polling")
        ;

        // TODO: If successful, delete/move file from bucket


        from("direct:getAllBlobs")
                .to("log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true")
                .bean("blobStoreService", "getAllBlobs")
                .to("log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true")
                .routeId("blobstore-list");
    }
}
