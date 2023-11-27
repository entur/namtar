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

package org.entur.namtar.metrics;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

import static org.entur.namtar.routes.api.MappingRoute.ET_CLIENT_NAME_HEADER;

@Component
public class PrometheusMetricsService extends PrometheusMeterRegistry {


    private static final String METRICS_PREFIX = "app.namtar.";

    private static final String DATA_CREATED_COUNTER_NAME = METRICS_PREFIX + "data.created";
    private static final String DATA_SEARCH_COUNTER_NAME = METRICS_PREFIX + "data.search";

    public PrometheusMetricsService() {
        super(PrometheusConfig.DEFAULT);
    }

    @PreDestroy
    public void shutdown() {
        this.close();
    }

    @Override
    public String scrape() {
        update();
        return super.scrape();
    }

    public void markNewDSJ(boolean createdNewOriginalDatedServiceJourney) {
        List<Tag> tags = new ArrayList<>();
        tags.add(new ImmutableTag("newOriginalDSJ", "" + createdNewOriginalDatedServiceJourney));
        counter(DATA_CREATED_COUNTER_NAME, tags).increment();
    }

    public void update() {
//Do nothing
    }

    public void markLookup(SearchType searchType, String codespace) {
        List<Tag> tags = new ArrayList<>();
        tags.add(new ImmutableTag("searchType", searchType.name()));
        if (codespace != null) {
            tags.add(new ImmutableTag("codespace", codespace));
        }
        if (MDC.get(ET_CLIENT_NAME_HEADER) != null) {
            tags.add(new ImmutableTag(ET_CLIENT_NAME_HEADER, MDC.get(ET_CLIENT_NAME_HEADER)));
        }
        counter(DATA_SEARCH_COUNTER_NAME, tags).increment();
    }
}
