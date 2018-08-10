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

public class ServiceJourney {

    private String serviceJourneyId;

    private String departureDate;

    private String privateCode;

    @JsonIgnore
    private String departureTime;

    @JsonIgnore
    private String lineRef;

    private String version;

    public ServiceJourney(String serviceJourneyId, String version, String privateCode, String lineRef, String departureDate, String departureTime) {
        this.serviceJourneyId = serviceJourneyId;
        this.version = version;
        this.privateCode = privateCode;
        this.lineRef = lineRef;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceJourney that = (ServiceJourney) o;

        return new EqualsBuilder()
                .append(serviceJourneyId, that.serviceJourneyId)
                .append(privateCode, that.privateCode)
                .append(lineRef, that.lineRef)
                .append(departureDate, that.departureDate)
                .append(departureTime, that.departureTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(serviceJourneyId)
                .append(privateCode)
                .append(lineRef)
                .append(departureDate)
                .append(departureTime)
                .toHashCode();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
