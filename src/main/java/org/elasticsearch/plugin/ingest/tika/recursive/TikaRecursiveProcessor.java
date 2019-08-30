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
import org.apache.tika.sax.ContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.metadata.Metadata;

import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.elasticsearch.ingest.ConfigurationUtils.readOptionalStringProperty;

public class TikaRecursiveProcessor extends AbstractProcessor {

    private static final String XHTML = "http://www.w3.org/1999/xhtml";
    public static final String TYPE = "tika_recursive";

    private final Tika tika;
    private final Logger logger = LogManager.getLogger(getClass());
    private final String handlerType;

    public TikaRecursiveProcessor(String tag, String handlerType) throws IOException {
        super(tag);
        this.tika = new Tika();
        this.handlerType = handlerType;
        logger.debug("Initialized TikaRecursiveProcessor");
    }

    public TikaRecursiveProcessor(String tag) throws IOException {
        super(tag);
        this.tika = new Tika();
        this.handlerType = null;
        logger.debug("Initialized TikaRecursiveProcessor");
    }

    private static class ToXMLNoHeadContentHandler extends ToHTMLContentHandler {
        boolean inHead = false;
        boolean seenBody = false;

        public ToXMLNoHeadContentHandler(OutputStream stream, String encoding)
                throws UnsupportedEncodingException {
            super(stream, encoding);
        }

        public ToXMLNoHeadContentHandler() {
            super();
        }

        @Override
        public void ignorableWhitespace (char ch[], int start, int length) throws SAXException
        {
            if (seenBody) {
                super.ignorableWhitespace(ch, start, length);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
        {
            if (uri.equals(XHTML) && localName == "head") {
                inHead = true;
            } else {
                if (uri.equals(XHTML) && localName == "body") {
                    seenBody = true;
                }
                if (!inHead) {
                    super.startElement(uri, localName, qName, atts);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if (uri.equals(XHTML) && localName == "head") {
                inHead = false;
            } else {
                if (!inHead) {
                    super.endElement(uri, localName, qName);
                }
            }
        }
    }

    private static class CHFactory implements ContentHandlerFactory {

        private static final long serialVersionUID = 1L;

        @Override
        public ContentHandler getNewContentHandler() {
            return new ToXMLNoHeadContentHandler();
        }

        @Override
        public ContentHandler getNewContentHandler(OutputStream os, String encoding)
                throws UnsupportedEncodingException {
            return getNewContentHandler(os, Charset.forName(encoding));
        }

        @Override
		public ContentHandler getNewContentHandler(OutputStream os, Charset charset) {
            try {
                return new ToXMLNoHeadContentHandler(os, charset.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("couldn't find charset for name: "+charset);
            }
		}
    }

    private void logFor(Metadata m) {
        if (logger.isDebugEnabled()) {
            String content = m.get("X-TIKA:content");
            if (content != null) {
                logger.debug("X-TIKA:content: {}", content);
            }
        }
        for (String name : m.names()) {
            if (name.startsWith("X-TIKA:EXCEPTION")) {
                for (String s : m.getValues(name)) {
                    logger.error("{}: {}", name, s);
                }
            }
        }
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        byte[] bytes = ingestDocument.getFieldValueAsBytes("data");
        ingestDocument.setFieldValue("data", bytes);

        ContentHandlerFactory factory;
        if (handlerType != null) {
            BasicContentHandlerFactory.HANDLER_TYPE type = BasicContentHandlerFactory.parseHandlerType(handlerType, BasicContentHandlerFactory.HANDLER_TYPE.HTML);
            factory = new BasicContentHandlerFactory(type, -1);
        } else {
            factory = new CHFactory();
        }

        Metadata metadata = new Metadata();
        ParseContext ctx = new ParseContext();

        RecursiveParserWrapperHandler wrapperHandler =  new RecursiveParserWrapperHandler(factory, -1);
        RecursiveParserWrapper wrapper = new RecursiveParserWrapper(tika.getParser());
        wrapper.parse(new ByteArrayInputStream(bytes), wrapperHandler, metadata, ctx);

        List<Metadata> metadatas = wrapperHandler.getMetadataList();
        Iterator<Metadata> i = metadatas.iterator();
        Metadata rootMetadata = i.next();
        logFor(rootMetadata);
        
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

        boolean first = true;
        while (i.hasNext()) { 
            Metadata attachment = i.next();
            logFor(attachment);
            Map<String, Object> meta = new HashMap<>();
            for (String name : attachment.names()) {
                String[] values = attachment.getValues(name);
                if (values.length > 1) {
                    meta.put(name, Arrays.asList(values));
                } else {
                    meta.put(name, values[0]);
                }
            }
            if (first) {
                ingestDocument.setFieldValue("X-TIKA:embedded_resources", meta);
            } else {
                ingestDocument.appendFieldValue("X-TIKA:embedded_resources", meta);
            }
            first = false;
        }

        // ingestDocument.removeField("data");
        // Writer writer = new OutputStreamWriter(System.out);
        // JsonMetadataList.setPrettyPrinting(true);
        // JsonMetadataList.toJson(metadatas, writer);
        // writer.flush();

        return ingestDocument;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory implements Processor.Factory {
        @Override
        public TikaRecursiveProcessor create(Map<String, Processor.Factory> factories, String tag, Map<String, Object> config)
            throws Exception
        {
            String handler = readOptionalStringProperty(TYPE, tag, config, "handler_type");
            return new TikaRecursiveProcessor(tag, handler);
        }
    }
}
