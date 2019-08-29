/*
 * Copyright [2018] [Lawrence D'Anna]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.elasticsearch.plugin.ingest.tika.recursive;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;

public class TikaRecursiveProcessorTests extends ESTestCase {

    public void testSimpleText() throws Exception {
      byte[] bytes = "Hello, I am just a simple string".getBytes();

      Map<String, Object> document = new HashMap<>();
      document.put("data", bytes);
      IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

      TikaRecursiveProcessor processor = new TikaRecursiveProcessor(randomAlphaOfLength(10));
      Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

      assertThat((String) data.get("X-TIKA:content"), containsString("I am just a simple string"));
    }

    public void testEmailWithTarball() throws Exception {
        byte[] bytes = IOUtils.toByteArray(TikaRecursiveProcessorTests.class.getResourceAsStream("/lol.eml"));

        Map<String, Object> document = new HashMap<>();
        document.put("data", bytes);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        TikaRecursiveProcessor processor = new TikaRecursiveProcessor(randomAlphaOfLength(10));
        Map<String, Object> data = processor.execute(ingestDocument).getSourceAndMetadata();

        // for (String key : ingestDocument.getSourceAndMetadata().keySet()) {
        //   System.out.print("K " + key);
        // }

        assertThat(data, hasKey("X-TIKA:content"));
        assertThat(data, hasKey("subject"));
        assertThat(data.get("subject"), is("lol ololool"));
        assertThat(data.get("Message:From-Email"), is("larry@elder-gods.org"));

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) data.get("Message:Raw-Header:Received");
        assertThat(list.size(), is(3));
    }
}
