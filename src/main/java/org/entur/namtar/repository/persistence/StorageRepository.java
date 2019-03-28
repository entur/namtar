/*
 *
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

package org.entur.namtar.repository.persistence;

import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.SourceFile;

import java.util.Collection;

public interface StorageRepository {

    void save(DatedServiceJourney journey);

    DatedServiceJourney findByServiceJourneyIdAndDate(String serviceJourneyId, String departureDate);

    DatedServiceJourney findByDatedServiceJourneyId(String datedServiceJourneyId);

    Collection<DatedServiceJourney> findByOriginalDatedServiceJourneyId(String originalDatedServiceJourneyId);

    DatedServiceJourney findByPrivateCodeDepartureDate(String privateCode, String departureDate);

    SourceFile findSourceFileByName(String sourceFileName);

    void save(SourceFile sourceFile);

    long findMaxCreationNumber();
}
