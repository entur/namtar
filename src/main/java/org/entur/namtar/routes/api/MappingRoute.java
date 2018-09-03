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
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.apache.camel.Exchange;
import org.apache.camel.model.rest.RestParamType;
import org.entur.namtar.routes.RestRouteBuilder;
import org.entur.namtar.services.DatedServiceJourneyService;
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
        mapper.registerModule(new JaxbAnnotationModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


        //TODO: Handle request for multiple DatedServiceJourneys

        rest("/api")
            .get("/{serviceJourneyId}/{version}/{date}").produces("text/json").to("direct:lookup.servicejourney.version.date")
                .param().required(true).name("serviceJourneyId").type(RestParamType.path).description("The id of the serviceJourney to look up").dataType("string").endParam()
                .param().required(true).name("version").type(RestParamType.path).description("Specific version or `latest`").dataType("string").endParam()
                .param().required(true).name("date").type(RestParamType.path).description("Date").dataType("string").endParam()

            .get("/{datedServiceJourneyId}").produces("text/json").to("direct:lookup.datedservicejourney.version.date")
                .param().required(true).name("datedServiceJourneyId").type(RestParamType.path).description("DatedServiceJourney to lookup").dataType("string").endParam()
        ;

        from("direct:lookup.datedservicejourney.version.date")
                .bean(repository, "findServiceJourneysByDatedServiceJourney(${header.datedServiceJourneyId})")
                .to("direct:createResponse")
                .routeId("namtar.datedServiceJourney")
        ;

        from("direct:lookup.servicejourney.version.date")
                .bean(repository, "findDatedServiceJourneys(${header.serviceJourneyId}, ${header.version}, ${header.date})")
                .to("direct:createResponse")
                .routeId("namtar.serviceJourney")
        ;

        from("direct:createResponse")
            .choice()
                .when(body().isNull())
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("404"))
                .otherwise()
                    .bean(mapper, "writeValueAsString(${body})")
            .endChoice()
            .routeId("namtar.createResponse")
        ;
    }
}
