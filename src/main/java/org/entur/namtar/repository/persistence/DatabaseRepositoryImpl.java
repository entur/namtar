/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *  https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.namtar.repository.persistence;

import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.SourceFile;

import java.util.Collection;

public class DatabaseRepositoryImpl implements StorageRepository {

    private final DatedServiceJourneyRepository datedServiceJourneyRepository;
    private final SourceFileRepository sourceFileRepository;

    public DatabaseRepositoryImpl(DatedServiceJourneyRepository repository, SourceFileRepository sourceFileRepository) {
        this.datedServiceJourneyRepository = repository;
        this.sourceFileRepository = sourceFileRepository;
    }

    @Override
    public void save(DatedServiceJourney journey) {
        datedServiceJourneyRepository.save(journey);
    }

    @Override
    public DatedServiceJourney findByServiceJourneyIdAndDate(String serviceJourneyId, String departureDate) {
        return datedServiceJourneyRepository.findByServiceJourneyIdAndDepartureDate(serviceJourneyId, departureDate);
    }

    @Override
    public DatedServiceJourney findByDatedServiceJourneyId(String datedServiceJourneyId) {
        return datedServiceJourneyRepository.findByDatedServiceJourneyId(datedServiceJourneyId);
    }

    @Override
    public Collection<DatedServiceJourney> findByOriginalDatedServiceJourneyId(String originalDatedServiceJourneyId) {
        return datedServiceJourneyRepository.findByOriginalDatedServiceJourneyIdOrderByPublicationTimestampDesc(originalDatedServiceJourneyId);
    }

    @Override
    public DatedServiceJourney findByPrivateCodeDepartureDate(String privateCode, String departureDate) {
        return datedServiceJourneyRepository.findFirstByPrivateCodeAndDepartureDateOrderByCreationNumberAsc(privateCode, departureDate);
    }

    @Override
    public SourceFile findSourceFileByName(String sourceFileName) {
        return sourceFileRepository.findSourceFileBySourceFileName(sourceFileName);
    }

    @Override
    public void save(SourceFile sourceFile) {
        sourceFileRepository.save(sourceFile);
    }

    @Override
    public long findNextCreationNumber() {

        return datedServiceJourneyRepository.findMaxCreationNumber();
    }
}
