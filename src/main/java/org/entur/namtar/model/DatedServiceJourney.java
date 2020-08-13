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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Timestamp;

@Entity
@Table(indexes = {@Index(name = "serviceJourney_date_idx", columnList = "serviceJourneyId, departureDate"),
                    @Index(name = "privateCode_date_idx", columnList = "privateCode, departureDate")}
        )
public class DatedServiceJourney {

    @Id
    @GeneratedValue
    private Long id;

    @Transient
    private int hashcode;

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
    private long creationNumber;

    @JsonIgnore
    private Timestamp createdDate;

    public DatedServiceJourney() {

    }

    public DatedServiceJourney(String serviceJourneyId, Integer version, String privateCode, String lineRef, String departureDate, String departureTime) {
        this(null, serviceJourneyId, version, privateCode, lineRef, departureDate, departureTime);
    }
    public DatedServiceJourney(String datedServiceJourneyId, String serviceJourneyId, Integer version, String privateCode, String lineRef, String departureDate, String departureTime) {
        this.datedServiceJourneyId = datedServiceJourneyId;
        this.serviceJourneyId = serviceJourneyId;
        this.version = version;
        this.privateCode = privateCode;
        this.lineRef = lineRef;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
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
                .append(creationNumber, that.creationNumber)
                .append(id, that.id)
                .append(serviceJourneyId, that.serviceJourneyId)
                .append(departureDate, that.departureDate)
                .append(privateCode, that.privateCode)
                .append(departureTime, that.departureTime)
                .append(lineRef, that.lineRef)
                .append(version, that.version)
                .append(datedServiceJourneyId, that.datedServiceJourneyId)
                .append(publicationTimestamp, that.publicationTimestamp)
                .append(sourceFileName, that.sourceFileName)
                .append(originalDatedServiceJourneyId, that.originalDatedServiceJourneyId)
                .append(createdDate, that.createdDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        if (hashcode < 0) {
            hashcode = new HashCodeBuilder(17, 37)
                    .append(id)
                    .append(serviceJourneyId)
                    .append(departureDate)
                    .append(privateCode)
                    .append(departureTime)
                    .append(lineRef)
                    .append(version)
                    .append(datedServiceJourneyId)
                    .append(publicationTimestamp)
                    .append(sourceFileName)
                    .append(originalDatedServiceJourneyId)
                    .append(creationNumber)
                    .append(createdDate)
                    .toHashCode();
        }
        return hashcode;
    }

    public void setCreationNumber(long creationNumber) {
        this.creationNumber = creationNumber;
    }

    public long getCreationNumber() {
        return creationNumber;
    }
}
