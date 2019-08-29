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

import org.apache.tika.Tika;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.metadata.serialization.JsonMetadataList;
import org.apache.tika.metadata.Metadata;

import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.xml.sax.ContentHandler;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class TikaRecursiveProcessor extends AbstractProcessor {

    public static final String TYPE = "tika_recursive";
    private final Tika tika;


    public TikaRecursiveProcessor(String tag) throws IOException {
        super(tag);
        this.tika = new Tika();
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {

        //byte[] bytes = ingestDocument.getFieldValueAsBytes(field);
        //byte[] bytes = "foo bar baz".getBytes();
        //byte[] bytes = Files.readAllBytes(Paths.get("/Users/lawrence_danna/Desktop/lol.eml"));

        byte[] bytes = ingestDocument.getFieldValueAsBytes("data");

        ingestDocument.setFieldValue("FOO", "BAR");
                
        BasicContentHandlerFactory factory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);
        Metadata metadata = new Metadata();
        ParseContext ctx = new ParseContext();

        RecursiveParserWrapperHandler wrapperHandler =  new RecursiveParserWrapperHandler(factory, -1);
        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(tika.getParser());
        wrapper.parse(new ByteArrayInputStream(bytes), wrapperHandler, metadata, ctx);

        List<Metadata> metadatas = wrapperHandler.getMetadataList();
        Iterator<Metadata> i = metadatas.iterator();
        Metadata rootMetadata = i.next();
        
        for (String name : rootMetadata.names()) { 
            boolean first = true;
            for (String value : rootMetadata.getValues(name)) { 
                if (first) { 
                    ingestDocument.setFieldValue(name, value);
                } else { 
                    ingestDocument.appendFieldValue(name, value);
                }
                first = false;
            }
        }

        while (i.hasNext()) { 
            Metadata attachment = i.next();
        }

        Writer writer = new OutputStreamWriter(System.out);
        JsonMetadataList.setPrettyPrinting(true);
        JsonMetadataList.toJson(metadatas, writer);
        writer.flush();

        return ingestDocument;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {
        @Override
        public TikaRecursiveProcessor create(Map<String, Processor.Factory> factories, String tag, Map<String, Object> config)
            throws Exception {
            return new TikaRecursiveProcessor(tag);
        }
    }
}
