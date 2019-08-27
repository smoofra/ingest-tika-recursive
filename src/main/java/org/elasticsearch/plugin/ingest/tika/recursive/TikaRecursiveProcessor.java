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

import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.Tika;
import org.apache.tika.metadata.serialization.JsonMetadataList;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;

import java.util.Map;

import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class TikaRecursiveProcessor extends AbstractProcessor {

    public static final String TYPE = "tika_recursive";

    private final String field;
    private final String targetField;
    private final Tika tika;


    public TikaRecursiveProcessor(String tag, String field, String targetField) throws IOException {
        super(tag);
        this.field = field;
        this.targetField = targetField;

        // Parser parsers[] = new Parser[] {
        //     new org.apache.tika.parser.html.HtmlParser(),
        //     new org.apache.tika.parser.rtf.RTFParser(),
        //     new org.apache.tika.parser.pdf.PDFParser(),
        //     new org.apache.tika.parser.txt.TXTParser(),
        //     new org.apache.tika.parser.microsoft.OfficeParser(),
        //     new org.apache.tika.parser.microsoft.OldExcelParser(),
        //     new org.apache.tika.parser.odf.OpenDocumentParser(),
        //     new org.apache.tika.parser.iwork.IWorkPackageParser(),
        //     new org.apache.tika.parser.xml.DcXMLParser(),
        //     new org.apache.tika.parser.epub.EpubParser(),
        // };
        //
        // AutoDetectParser autoParser = new AutoDetectParser(parsers);
        // this.tika = new Tika(autoParser.getDetector(), autoParser);

        this.tika = new Tika();
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {

        //byte[] bytes = ingestDocument.getFieldValueAsBytes(field);

        byte[] bytes = "foo bar baz".getBytes();

        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(tika.getParser());
        BasicContentHandlerFactory factory = new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.HTML, -1);
        RecursiveParserWrapperHandler handler =  new RecursiveParserWrapperHandler(factory, -1);
        Metadata metadata = new Metadata();
        ParseContext ctx = new ParseContext();
        wrapper.parse(new ByteArrayInputStream(bytes), handler, metadata, ctx);

        JsonMetadataList.toJson(handler.getMetadataList(), new OutputStreamWriter(System.out));

        String content = ingestDocument.getFieldValue(field, String.class);
        ingestDocument.setFieldValue(targetField, content);
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
            String field = readStringProperty(TYPE, tag, config, "field");
            String targetField = readStringProperty(TYPE, tag, config, "target_field", "default_field_name");

            return new TikaRecursiveProcessor(tag, field, targetField);
        }
    }
}
