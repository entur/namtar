/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.entur.namtar.routes.kafka;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.dataformat.avro.AvroDataFormat;
import org.entur.namtar.model.avro.DatedServiceJourney;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaRoute extends RouteBuilder {

    private static String KAFKA_PUBLISH_ROUTE_NAME = "direct:namtar.kafka.publish";

    @Value("${namtar.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Override
    public void configure(){

        if (kafkaEnabled) {
            /*
             * Publishes data to Kafka
             */
            from(KAFKA_PUBLISH_ROUTE_NAME)
                    .marshal(new AvroDataFormat())
                    .to("kafka:{{namtar.kafka.topic.name}}?brokers={{namtar.kafka.brokers}}"
                    )
                    .routeId("namtar.kafka.publish");
        }

    }

    public void publishToKafka(org.entur.namtar.model.DatedServiceJourney dsj) {
        if (!kafkaEnabled) {
            return;
        }
        ProducerTemplate producerTemplate = getContext().createProducerTemplate();

        Map<String, Object> headers = new HashMap<>();

        headers.put(KafkaConstants.PARTITION_KEY, 0);
        headers.put(KafkaConstants.KEY, "1");

        DatedServiceJourney avroDatedServiceJourney = DatedServiceJourney.newBuilder()
                .setDatedServiceJourneyId(dsj.getDatedServiceJourneyId())
                .setDepartureDate(dsj.getDepartureDate())
                .setDepartureTime(dsj.getDepartureTime())
                .setLineRef(dsj.getLineRef())
                .setOriginalDatedServiceJourneyId(dsj.getOriginalDatedServiceJourneyId())
                .setPrivateCode(dsj.getPrivateCode())
                .setPublicationTimestamp(dsj.getPublicationTimestamp())
                .setServiceJourneyId(dsj.getServiceJourneyId())
                .setSourceFileName(dsj.getSourceFileName())
                .setVersion(dsj.getVersion())
                .build();

        producerTemplate.sendBodyAndHeaders(KAFKA_PUBLISH_ROUTE_NAME, avroDatedServiceJourney, headers);

    }
}