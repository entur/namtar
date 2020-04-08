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

package org.entur.namtar.repository.blobstore;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.Iterator;

@Repository
@Profile("in-memory-blobstore")
public class InMemoryBlobStoreRepository implements BlobStoreRepository {

    @Override
    public Iterator<Blob> listBlobs(String prefix) {
        return null;
    }

    @Override
    public InputStream getBlob(String objectName) {
        return null;
    }

    @Override
    public void setStorage(Storage storage) {
        // Ignored - is in memory
    }

    @Override
    public void setContainerName(String containerName) {
        // Ignored - is in memory
    }

}
