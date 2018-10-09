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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DatedServiceJourneyRepository extends JpaRepository<DatedServiceJourney, Long> {

    DatedServiceJourney findByServiceJourneyIdAndDepartureDate(String serviceJourneyId, String departureDate);

    DatedServiceJourney findByDatedServiceJourneyId(String datedServiceJourneyId);

    List<DatedServiceJourney> findByOriginalDatedServiceJourneyIdOrderByPublicationTimestampDesc(String originalDatedServiceJourneyId);

    List<DatedServiceJourney> findDatedServiceJourneysByDepartureDateGreaterThan(String departureDate);

    List<DatedServiceJourney> findDatedServiceJourneysByCreatedDateAfter(Date createdDate);

    DatedServiceJourney findFirstByPrivateCodeAndDepartureDateOrderByCreationNumberAsc(String privateCode, String departureDate);

    @Query("select count(sourceFileName) from DatedServiceJourney where sourceFileName = ?1")
    int findDistinctFirstBySourceFileName(String sourceFileName);

    @Query("SELECT coalesce(max(creationNumber), 0) FROM DatedServiceJourney")
    long findMaxCreationNumber();

    @Query("SELECT distinct sourceFileName FROM DatedServiceJourney")
    List<String> findDistinctSourceFileNames();
}