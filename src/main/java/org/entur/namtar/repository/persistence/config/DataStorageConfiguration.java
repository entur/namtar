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
 */

package org.entur.namtar.repository.persistence.config;


import org.entur.namtar.repository.persistence.DatabaseRepositoryImpl;
import org.entur.namtar.repository.persistence.DatedServiceJourneyRepository;
import org.entur.namtar.repository.persistence.SourceFileRepository;
import org.entur.namtar.services.DataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DataStorageConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public DataStorageService createDatabaseStorageService(@Autowired DatedServiceJourneyRepository datedServiceJourneyRepository,
                                                           @Autowired SourceFileRepository sourceFileRepository) {
        logger.info("Initializing DataStorageService with DatabaseRepositoryImpl");
        return new DataStorageService(new DatabaseRepositoryImpl(datedServiceJourneyRepository, sourceFileRepository));
    }
}
