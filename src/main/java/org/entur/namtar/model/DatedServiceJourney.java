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

package org.entur.namtar.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class DatedServiceJourney {


    private String datedServiceJourneyId;

    private LocalDateTime publicationTimestamp;

    private int hashcode;

    public DatedServiceJourney() {
    }

    @Override
    public String toString() {
        return datedServiceJourneyId + ", " + publicationTimestamp;
    }

    public String getDatedServiceJourneyId() {
        return datedServiceJourneyId;
    }

    public void setDatedServiceJourneyId(String datedServiceJourneyId) {
        this.datedServiceJourneyId = datedServiceJourneyId;
    }


    public void setPublicationTimestamp(LocalDateTime publicationTimestamp) {
        this.publicationTimestamp = publicationTimestamp;
    }

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    public LocalDateTime getPublicationTimestamp() {
        return publicationTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DatedServiceJourney that = (DatedServiceJourney) o;

        return new EqualsBuilder()
                .append(datedServiceJourneyId, that.datedServiceJourneyId)
                .append(publicationTimestamp, that.publicationTimestamp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = new HashCodeBuilder(17, 37)
                    .append(datedServiceJourneyId)
                    .append(publicationTimestamp)
                    .toHashCode();
        }
        return hashcode;
    }
}
