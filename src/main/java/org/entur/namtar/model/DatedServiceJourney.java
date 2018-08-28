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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DatedServiceJourney {


    private String serviceJourneyId;

    private String departureDate;

    private String privateCode;

    private String departureTime;

    private String lineRef;

    private Integer version;

    private String datedServiceJourneyId;

    private String publicationTimestamp;

    private String sourceFileName;

    private String originalDatedServiceJourneyId;

    @JsonIgnore
    private long datedServiceJourneyCreationNumber;

    public DatedServiceJourney() {

    }


    public String getServiceJourneyId() {
        return serviceJourneyId;
    }

    public void setServiceJourneyId(String serviceJourneyId) {
        this.serviceJourneyId = serviceJourneyId;
    }

    public String getLineRef() {
        return lineRef;
    }

    public void setLineRef(String lineRef) {
        this.lineRef = lineRef;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getPrivateCode() {
        return privateCode;
    }

    public void setPrivateCode(String privateCode) {
        this.privateCode = privateCode;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }


    private int hashcode;

    public DatedServiceJourney(String datedServiceJourneyId, String originalDatedServiceJourneyId, String publicationTimestamp, String sourceFileName) {
        this.datedServiceJourneyId = datedServiceJourneyId;
        this.originalDatedServiceJourneyId = originalDatedServiceJourneyId;
        this.publicationTimestamp = publicationTimestamp;
        this.sourceFileName = sourceFileName;
    }

    @Override
    public String toString() {
        return datedServiceJourneyId + ", " + publicationTimestamp;
    }

    public String getDatedServiceJourneyId() {
        return datedServiceJourneyId;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getPublicationTimestamp() {
        return publicationTimestamp;
    }


    public void setDatedServiceJourneyId(String datedServiceJourneyId) {
        this.datedServiceJourneyId = datedServiceJourneyId;
    }

    public void setPublicationTimestamp(String publicationTimestamp) {
        this.publicationTimestamp = publicationTimestamp;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public void setOriginalDatedServiceJourneyId(String originalDatedServiceJourneyId) {
        this.originalDatedServiceJourneyId = originalDatedServiceJourneyId;
    }

    public String getOriginalDatedServiceJourneyId() {
        return originalDatedServiceJourneyId;
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
                .append(originalDatedServiceJourneyId, that.originalDatedServiceJourneyId)
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

    public void setDatedServiceJourneyCreationNumber(long datedServiceJourneyCreationNumber) {
        this.datedServiceJourneyCreationNumber = datedServiceJourneyCreationNumber;
    }

    public long getDatedServiceJourneyCreationNumber() {
        return datedServiceJourneyCreationNumber;
    }
}
