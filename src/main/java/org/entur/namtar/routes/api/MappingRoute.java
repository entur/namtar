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
import org.apache.camel.model.rest.RestOperationResponseMsgDefinition;
import org.apache.camel.model.rest.RestParamType;
import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.routes.RestRouteBuilder;
import org.entur.namtar.services.DatedServiceJourneyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;


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


        /*

           Because of routing-rules - the REST endpoints have been duplicated as a hack to allow endpoints
           in swagger-doc to be created correctly - i.e. without the "api"-prefix.

           When routing rules are changed, the duplicated endpoint should be removed

         */

        RestOperationResponseMsgDefinition notFoundResponse = new RestOperationResponseMsgDefinition();
        notFoundResponse.setMessage("Not found");
        notFoundResponse.code(404);

        RestOperationResponseMsgDefinition dsjResponse = new RestOperationResponseMsgDefinition();
        dsjResponse.responseModel(DatedServiceJourney.class);
        dsjResponse.code(200);

        RestOperationResponseMsgDefinition dsjResponseArray = new RestOperationResponseMsgDefinition();
        dsjResponseArray.responseModel(DatedServiceJourney[].class);
        dsjResponseArray.code(200);

        rest("")
                .tag("api")
                .apiDocs(Boolean.TRUE)
                .get("/{serviceJourneyId}/{version}/{date}").produces("text/json").to("direct:lookup.single.servicejourney.version.date")
                    .param().required(true).name("serviceJourneyId").type(RestParamType.path).description("The id of the serviceJourney to look up").dataType("string").endParam()
                    .param().required(true).name("version").type(RestParamType.path).description("Specific version or `latest`").dataType("string").endParam()
                    .param().required(true).name("date").type(RestParamType.path).description("Date - format YYYY-MM-DD").dataType("string").endParam()
                    .responseMessages(Arrays.asList(notFoundResponse, dsjResponse))

                .post("/query").type(ServiceJourneyParam[].class).consumes("text/json").produces("text/json")
                    .param().name("body").type(RestParamType.body).description("The ServiceJourneys to look up").endParam()
                    .responseMessage(dsjResponseArray)
                    .to("direct:lookup.multiple.servicejourneys.version.date")

                .get("/{datedServiceJourneyId}").produces("text/json").to("direct:lookup.single.datedservicejourney")
                    .apiDocs(Boolean.FALSE) // Deprecated service endpoint - ignore this in swagger doc
                .get("/dated/{datedServiceJourneyId}").produces("text/json").to("direct:lookup.single.datedservicejourney")
                    .param().required(true).name("datedServiceJourneyId").type(RestParamType.path).description("DatedServiceJourney to lookup").dataType("string").endParam()
                    .responseMessages(Arrays.asList(notFoundResponse, dsjResponse))

                .get("/original/{originalDatedServiceJourneyId}").produces("text/json").to("direct:lookup.original.datedservicejourney")
                    .param().required(true).name("originalDatedServiceJourneyId").type(RestParamType.path).description("OriginalDatedServiceJourney to lookup").dataType("string").endParam()
                    .responseMessage(dsjResponseArray)

                .get("/privatecode/{privateCode}/{date}").produces("text/json").to("direct:lookup.privatecode.date")
                    .param().required(true).name("privateCode").type(RestParamType.path).description("PrivateCode to lookup").dataType("string").endParam()
                    .param().required(true).name("date").type(RestParamType.path).description("Date - format YYYY-MM-DD").dataType("string").endParam()
                    .responseMessage(dsjResponseArray)

                .post("/reverse-query").type(DatedServiceJourneyParam[].class).consumes("text/json").produces("text/json")
                    .param().name("body").type(RestParamType.body).description("The DatedServiceJourneys to look up").endParam()
                    .responseMessage(dsjResponseArray)
                    .to("direct:lookup.multiple.datedservicejourneys");

        rest("/api")
            .apiDocs(Boolean.FALSE)
            .get("/{serviceJourneyId}/{version}/{date}").produces("text/json").to("direct:lookup.single.servicejourney.version.date")
            .post("/query").type(ServiceJourneyParam[].class).consumes("text/json").produces("text/json")
                .to("direct:lookup.multiple.servicejourneys.version.date")
            .get("/{datedServiceJourneyId}").produces("text/json").to("direct:lookup.single.datedservicejourney")
            .get("/dated/{datedServiceJourneyId}").produces("text/json").to("direct:lookup.single.datedservicejourney")
            .get("/original/{originalDatedServiceJourneyId}").produces("text/json").to("direct:lookup.original.datedservicejourney")
            .get("/privatecode/{privateCode}/{date}").produces("text/json").to("direct:lookup.privatecode.date")
            .post("/reverse-query").type(DatedServiceJourneyParam[].class).consumes("text/json").produces("text/json")
                .to("direct:lookup.multiple.datedservicejourneys")
        ;

        from("direct:lookup.single.datedservicejourney")
                .bean(repository, "findServiceJourneyByDatedServiceJourney(${header.datedServiceJourneyId})")
                .to("direct:createResponse")
                .routeId("namtar.single.datedServiceJourney")
        ;


        from("direct:lookup.privatecode.date")
                .bean(repository, "findServiceJourneyByPrivateCodeDepartureDate(${header.privateCode}, ${header.date})")
                .to("direct:createResponse")
                .routeId("namtar.privatecode.date")
        ;

        from("direct:lookup.original.datedservicejourney")
                .bean(repository, "findServiceJourneysByOriginalDatedServiceJourney(${header.originalDatedServiceJourneyId})")
                .to("direct:createResponse")
                .routeId("namtar.original.datedServiceJourney")
        ;

        from("direct:lookup.multiple.datedservicejourneys")
                .process((Exchange p) -> {
                    p.getOut().setBody(mapper.readValue(p.getIn().getBody(InputStream.class), DatedServiceJourneyParam[].class));
                })
                .bean(repository, "findServiceJourneysByDatedServiceJourneys(${body})")
                .to("direct:createResponse")
                .routeId("namtar.multiple.datedServiceJourneys")
        ;

        from("direct:lookup.single.servicejourney.version.date")
                .bean(repository, "findDatedServiceJourney(${header.serviceJourneyId}, ${header.version}, ${header.date})")
                .to("direct:createResponse")
                .routeId("namtar.single.serviceJourney")
        ;


        from("direct:lookup.multiple.servicejourneys.version.date")
                .process((Exchange p) -> {
                    p.getOut().setBody(mapper.readValue(p.getIn().getBody(InputStream.class), ServiceJourneyParam[].class));
                })
                .bean(repository, "findDatedServiceJourneys(${body})")
                .to("direct:createResponse")
                .routeId("namtar.multiple.serviceJourneys")
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
