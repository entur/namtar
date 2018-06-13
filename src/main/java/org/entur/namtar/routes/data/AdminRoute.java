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

import org.apache.camel.Exchange;
import org.entur.namtar.netex.NetexLoader;
import org.entur.namtar.routes.RestRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;


@Service
@Configuration
public class AdminRoute extends RestRouteBuilder {

    private final NetexLoader netexLoader;

    public AdminRoute(@Autowired NetexLoader netexLoader) {
        this.netexLoader = netexLoader;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        rest("/admin")
            .get("/update").to("direct:trigger.update")
        ;

        from("direct:trigger.update")
                .wireTap("direct:trigger.update.async")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("202"))
                .setBody(constant(null))
                .routeId("netex.update.trigger")
                ;

        from("direct:trigger.update.async")
                .bean(netexLoader, "loadNetexFromUrl(${header.url})")
                .routeId("netex.update.processing")
            ;

        // TODO: Fetch data from GCS Blobstore
        // Download data
        // Process netex-file
        // If successful, delete file from bucket
    }
}
