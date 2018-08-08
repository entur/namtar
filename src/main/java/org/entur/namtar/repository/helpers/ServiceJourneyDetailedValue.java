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

package org.entur.namtar.repository.helpers;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ServiceJourneyDetailedValue {

    final String privateCode, lineRef, departureDate;
    final int version;
    private int hashcode;

    public ServiceJourneyDetailedValue(int version, String privateCode, String lineRef, String departureDate) {
        this.version = version;
        this.privateCode = privateCode;
        this.lineRef = lineRef;
        this.departureDate = departureDate;
    }

    public String getPrivateCode() {
        return privateCode;
    }

    public String getLineRef() {
        return lineRef;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceJourneyDetailedValue that = (ServiceJourneyDetailedValue) o;

        return new EqualsBuilder()
                .append(version, that.version)
                .append(privateCode, that.privateCode)
                .append(lineRef, that.lineRef)
                .append(departureDate, that.departureDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        if (hashcode == 0) {
            hashcode = new HashCodeBuilder(17, 37)
                    .append(version)
                    .append(privateCode)
                    .append(lineRef)
                    .append(departureDate)
                    .toHashCode();
        }
        return hashcode;
    }

}
