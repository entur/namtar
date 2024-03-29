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

package org.entur.namtar.services;

import org.entur.namtar.metrics.PrometheusMetricsService;
import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.routes.api.DatedServiceJourneyParam;
import org.entur.namtar.routes.api.ServiceJourneyParam;
import org.entur.namtar.routes.kafka.KafkaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.entur.namtar.metrics.SearchType.DATED_SERVICE_JOURNEY;
import static org.entur.namtar.metrics.SearchType.ORIGINAL_DATED_SERVICE_JOURNEY;
import static org.entur.namtar.metrics.SearchType.PRIVATE_CODE_DEPARTURE_DATE;
import static org.entur.namtar.metrics.SearchType.SERVICE_JOURNEY_ID_DATE;

@Service
public class DatedServiceJourneyService {

    private final Logger logger = LoggerFactory.getLogger(DatedServiceJourneyService.class);


    private final DataStorageService storageService;

    private final PrometheusMetricsService metricsService;

    private final String GENERATED_ID_PREFIX;

    private long nextId;

    private KafkaPublisher kafkaNotifier;

    public DatedServiceJourneyService(@Autowired DataStorageService storageService,
                                      @Autowired KafkaPublisher kafkaNotifier,
                                      @Value("${namtar.generated.id.prefix}") String idPrefix,
                                      @Autowired PrometheusMetricsService metricsService) {
        logger.info("Initializing DatedServiceJourneyService");
        this.storageService = storageService;
        this.kafkaNotifier = kafkaNotifier;
        GENERATED_ID_PREFIX = idPrefix;
        this.metricsService = metricsService;
        updateNextCreationNumber();
        logger.info("Initializing DatedServiceJourneyService - done");
    }

    public void updateNextCreationNumber() {
        nextId = this.storageService.findNextCreationNumber();
    }

    public DatedServiceJourney createDatedServiceJourney(DatedServiceJourney serviceJourney, LocalDateTime publicationTimestamp, String sourceFileName) {

        long t1 = System.currentTimeMillis();

        // TODO: Handle versions

        DatedServiceJourney alreadyProcessed = storageService.findByServiceJourneyIdAndDate(serviceJourney.getServiceJourneyId(), serviceJourney.getDepartureDate());
        long t2 = System.currentTimeMillis();

        if (alreadyProcessed != null) {
            return null;
        }

        // Check to see if departure with same privateCode already exists...
        DatedServiceJourney datedServiceJourney = findServiceJourneyByPrivateCodeDepartureDate(serviceJourney.getPrivateCode(), serviceJourney.getDepartureDate());
        long t3 = System.currentTimeMillis();

        if (((t2-t1) > 10) && (t3-t2 > 10)) {
            logger.info("Check existing: serviceJourneyId: {} ms, privateCode: {} ms", (t2 - t1), (t3 - t2));
        }
        long creationNumber = nextId++;

        boolean createdNewOriginalDatedServiceJourney = false;
        boolean datedServiceJourneyIdIsProvided = false;

        final String generateDatedServiceJourneyId = generateDatedServiceJourneyId(creationNumber);

        if (serviceJourney.getDatedServiceJourneyId() != null &&
            !serviceJourney.getDatedServiceJourneyId().isEmpty()) {
            // DSJ is provided in the NeTEx-data
            datedServiceJourneyIdIsProvided = true;
        } else {
            // DSJ is not already set - generate a new one
            serviceJourney.setDatedServiceJourneyId(generateDatedServiceJourneyId);
        }

        if (datedServiceJourneyIdIsProvided && datedServiceJourney == null) {
            // DSJ not found by privateCode/serviceJourney+date
            // Lookup DSJ directly
            datedServiceJourney = findServiceJourneyByDatedServiceJourney(serviceJourney.getDatedServiceJourneyId());
            if (datedServiceJourney != null) {
                // DSJ is found - check if it already exists for another date
                if (! datedServiceJourney.getDepartureDate().equals(serviceJourney.getDepartureDate())) {
                    logger.warn("Ignoring DatedServiceJourney [{}] as it already exists for another departureDate [{}]", serviceJourney.getDatedServiceJourneyId(), datedServiceJourney.getDepartureDate());
                    return null;
                }
            }
        }

        String originalDatedServiceJourney;
        if (datedServiceJourney != null) {
            // ...exists - set original Id
            originalDatedServiceJourney = datedServiceJourney.getOriginalDatedServiceJourneyId();

            if (datedServiceJourneyIdIsProvided &&
                findServiceJourneyByDatedServiceJourney(serviceJourney.getDatedServiceJourneyId()) != null) {

                /*
                 * If the DSJ is provided, and has already been imported, we need to use the generated DSJ
                 */

                serviceJourney.setDatedServiceJourneyId(generateDatedServiceJourneyId);
            }
        } else {
            // ...does not exist - use current as original Id
            originalDatedServiceJourney = serviceJourney.getDatedServiceJourneyId();
            createdNewOriginalDatedServiceJourney = true;
        }

        DatedServiceJourney storageDatedServiceJourney = new DatedServiceJourney();
        storageDatedServiceJourney.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        storageDatedServiceJourney.setServiceJourneyId(serviceJourney.getServiceJourneyId());
        storageDatedServiceJourney.setDepartureDate(serviceJourney.getDepartureDate());
        storageDatedServiceJourney.setDepartureTime(serviceJourney.getDepartureTime());
        storageDatedServiceJourney.setPrivateCode(serviceJourney.getPrivateCode());
        storageDatedServiceJourney.setLineRef(serviceJourney.getLineRef());
        storageDatedServiceJourney.setVersion(serviceJourney.getVersion());
        storageDatedServiceJourney.setDatedServiceJourneyId(serviceJourney.getDatedServiceJourneyId());
        storageDatedServiceJourney.setCreationNumber(creationNumber);
        storageDatedServiceJourney.setOriginalDatedServiceJourneyId(originalDatedServiceJourney);
        storageDatedServiceJourney.setPublicationTimestamp(publicationTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        storageDatedServiceJourney.setSourceFileName(sourceFileName);

        if (createdNewOriginalDatedServiceJourney) {
            kafkaNotifier.publishToKafka(storageDatedServiceJourney);
        }
        metricsService.markNewDSJ(createdNewOriginalDatedServiceJourney);
        return storageDatedServiceJourney;
    }

    public DatedServiceJourney findServiceJourneyByPrivateCodeDepartureDate(String privateCode, String departureDate) {
        DatedServiceJourney byPrivateCodeDepartureDate = storageService.findByPrivateCodeDepartureDate(privateCode, departureDate);
        String codespace = null;
        if (byPrivateCodeDepartureDate != null) {
            codespace = byPrivateCodeDepartureDate.getServiceJourneyId().substring(0, 3);
        }
        metricsService.markLookup(PRIVATE_CODE_DEPARTURE_DATE, codespace);

        return byPrivateCodeDepartureDate;
    }

    public DatedServiceJourney findServiceJourneyByDatedServiceJourney(String datedServiceJourneyId) {

        DatedServiceJourney datedServiceJourney = storageService.findByDatedServiceJourneyId(datedServiceJourneyId);
        String codespace = null;
        if (datedServiceJourney != null) {
            codespace = datedServiceJourney.getServiceJourneyId().substring(0, 3);
        }
        metricsService.markLookup(DATED_SERVICE_JOURNEY, codespace);
        return datedServiceJourney;
    }


    public Collection<DatedServiceJourney> findServiceJourneysByOriginalDatedServiceJourney(String datedServiceJourneyId) {

        Collection<DatedServiceJourney> byOriginalDatedServiceJourneyId = storageService.findByOriginalDatedServiceJourneyId(datedServiceJourneyId);
        String codespace = null;
        if (byOriginalDatedServiceJourneyId != null && !byOriginalDatedServiceJourneyId.isEmpty()) {
            codespace = byOriginalDatedServiceJourneyId.iterator().next().getServiceJourneyId().substring(0, 3);
        }
        metricsService.markLookup(ORIGINAL_DATED_SERVICE_JOURNEY, codespace);


        return byOriginalDatedServiceJourneyId;
    }

    public List<DatedServiceJourney> findServiceJourneysByDatedServiceJourneys(DatedServiceJourneyParam... datedServiceJourneyParams) {
        List<DatedServiceJourney> result = new ArrayList<>();
        for (DatedServiceJourneyParam datedServiceJourneyParam : datedServiceJourneyParams) {
            DatedServiceJourney serviceJourney = findServiceJourneyByDatedServiceJourney(datedServiceJourneyParam.datedServiceJourneyId);
            if (serviceJourney != null) {
                result.add(serviceJourney);
            }
        }

        return result;
    }

    public DatedServiceJourney findDatedServiceJourney(String serviceJourneyId, String version, String departureDate) {
        metricsService.markLookup(SERVICE_JOURNEY_ID_DATE, serviceJourneyId.substring(0, 3));

        //TODO: Handle versions

        return storageService.findByServiceJourneyIdAndDate(serviceJourneyId, departureDate);
    }

    public List<DatedServiceJourney> findDatedServiceJourneys(ServiceJourneyParam... serviceJourneyParams) {
        List<DatedServiceJourney> result = new ArrayList<>();
        for (ServiceJourneyParam serviceJourneyParam : serviceJourneyParams) {
            DatedServiceJourney datedServiceJourney = findDatedServiceJourney(serviceJourneyParam.serviceJourneyId, null, serviceJourneyParam.departureDate);
            if (datedServiceJourney != null) {
                result.add(datedServiceJourney);
            }
        }

        return result;
    }

    private String generateDatedServiceJourneyId(long creationNumber) {
        return GENERATED_ID_PREFIX + creationNumber;
    }

    public DataStorageService getStorageService() {
        return storageService;
    }
}
