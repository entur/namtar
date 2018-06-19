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

package org.entur.namtar.routes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.entur.namtar.repository.DatedServiceJourneyService;
import org.entur.namtar.routes.RestRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;


@Service
@Configuration
public class MappingRoute extends RestRouteBuilder {

    @Autowired
    private final DatedServiceJourneyService repository;

    public MappingRoute(@Autowired DatedServiceJourneyService repository) {
        this.repository = repository;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        rest("/api")
            .get("/{serviceJourneyId}/{version}/{date}").produces("text/json").to("direct:lookup.servicejourney.version.date")
        ;

        from("direct:lookup.servicejourney.version.date")
                .bean(repository, "findDatedServiceJourneys(${header.serviceJourneyId}, ${header.version}, ${header.date})")
                .bean(mapper, "writeValueAsString(${body})")
        ;
    }
}
