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

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.entur.namtar.model.avro.DatedServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.Future;

@Service
public class KafkaPublisher {
    private final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);

    @Value("${namtar.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${namtar.kafka.topic.name:test}")
    private String topicName;

    @Value("${namtar.kafka.security.protocol}")
    private String securityProtocol;


    @Value("${namtar.kafka.security.sasl.mechanism}")
    private String saslMechanism;

    @Value("${namtar.kafka.sasl.username}")
    private String saslUsername;

    @Value("${namtar.kafka.sasl.password}")
    private String saslPassword;

    @Value("${namtar.kafka.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${namtar.kafka.brokers}")
    private String brokers;

    private KafkaProducer producer;

    @PostConstruct
    public void init() {
        if (!kafkaEnabled) {
            return;
        }
        KafkaConfiguration config = new KafkaConfiguration();

        Properties properties = config.createProducerProperties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);

        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

        properties.put("schema.registry.url",schemaRegistryUrl);

        // Schema registry authentication
        properties.put("basic.auth.credentials.source", "USER_INFO");
        properties.put("basic.auth.user.info", saslUsername.trim()+":"+saslPassword.trim());

// Security
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        properties.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        String jaasConfigContents = "org.apache.kafka.common.security.scram.ScramLoginModule required\nusername=\"%s\"\npassword=\"%s\";";
        properties.put(SaslConfigs.SASL_JAAS_CONFIG,
                String.format(jaasConfigContents, saslUsername.trim(), saslPassword.trim())
        );

        producer = new KafkaProducer(properties);

    }

    public void publishToKafka(org.entur.namtar.model.DatedServiceJourney dsj) {
        if (!kafkaEnabled) {
            log.debug("Push to Kafka is disabled, should have pushed [{}]", dsj);
            return;
        }

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

        Future future = producer.send(new ProducerRecord(topicName, avroDatedServiceJourney));

        Object result = null;
        try {
            result = future.get();
        } catch (Exception e) {
            result = e;
        } finally {
            log.info("Pushed avro-data {} to kafka, result: {}", dsj.getDatedServiceJourneyId(), result);
        }
    }

}
