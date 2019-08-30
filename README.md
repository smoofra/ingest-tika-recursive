# Recursive Tika Ingest Processor for Elasticsearch

This is similar to the `ingest-attachment` processor, except it also recursivly parses inner resources, like attachments or files within an zip archive.

## Usage

```
PUT _ingest/pipeline/tika
{
  "description": "tika",
  "processors": [
    {
      "tika_recursive" : {}
    }
  ]
}

PUT /my-index/_doc/1?pipeline=tika
{
  "data": "UEsDBAoAAAAAAL2pHU9HlyyyBwAAAAcAAAAHABwAZm9vLnR4dFVUCQADhaJoXY6iaF11eAsAAQTy/SI4BFAAAABmb29iYXIKUEsBAh4DCgAAAAAAvakdT0eXLLIHAAAABwAAAAcAGAAAAAAAAQAAAKSBAAAAAGZvby50eHRVVAUAA4WiaF11eAsAAQTy/SI4BFAAAABQSwUGAAAAAAEAAQBNAAAASAAAAAAA"
}

GET /my-index/_doc/1

=> 

{
  "_index" : "my-index",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 4,
  "_seq_no" : 3,
  "_primary_term" : 1,
  "found" : true,
  "_source" : {
    "X-Parsed-By" : [
      "org.apache.tika.parser.DefaultParser",
      "org.apache.tika.parser.pkg.PackageParser"
    ],
    "data" : "UEsDBAoAAAAAAL2pHU9HlyyyBwAAAAcAAAAHABwAZm9vLnR4dFVUCQADhaJoXY6iaF11eAsAAQTy/SI4BFAAAABmb29iYXIKUEsBAh4DCgAAAAAAvakdT0eXLLIHAAAABwAAAAcAGAAAAAAAAQAAAKSBAAAAAGZvby50eHRVVAUAA4WiaF11eAsAAQTy/SI4BFAAAABQSwUGAAAAAAEAAQBNAAAASAAAAAAA",
    "X-TIKA:embedded_resources" : {
      "date" : "2019-08-30T04:13:57Z",
      "X-Parsed-By" : [
        "org.apache.tika.parser.DefaultParser",
        "org.apache.tika.parser.csv.TextAndCSVParser"
      ],
      "resourceName" : "foo.txt",
      "dcterms:modified" : "2019-08-30T04:13:57Z",
      "Last-Modified" : "2019-08-30T04:13:57Z",
      "Last-Save-Date" : "2019-08-30T04:13:57Z",
      "embeddedRelationshipId" : "foo.txt",
      "meta:save-date" : "2019-08-30T04:13:57Z",
      "Content-Encoding" : "ISO-8859-1",
      "modified" : "2019-08-30T04:13:57Z",
      "X-TIKA:content" : """
<html xmlns="http://www.w3.org/1999/xhtml"><body><p>foobar
</p>
</body></html>
""",
      "Content-Length" : "7",
      "X-TIKA:embedded_resource_path" : "/foo.txt",
      "Content-Type" : "text/plain; charset=ISO-8859-1"
    },
    "X-TIKA:content" : """
<html xmlns="http://www.w3.org/1999/xhtml"><body><div class="embedded" id="foo.txt"></div>
<div class="package-entry"><h1>foo.txt</h1>
</div>
</body></html>
""",
    "Content-Type" : "application/zip"
  }
}

```

## Configuration

| Parameter | Use |
| --- | --- |
| handler_type   | (optional) Tika content handler type.  may be `xml`, `txt`, or `html`.   If blank it defaults to `html`, but with the `<head>` section stripped out.|

### mappings

In order to perform queries against the attachments, you can set up nested mappings, like this:

```
PUT my-index
{
  "mappings": {
    "properties": {
      "X-TIKA:embedded_resources":{"type": "nested"}
    }
  }
}
```

See: [nested datatype](https://www.elastic.co/guide/en/elasticsearch/reference/current/nested.html).


## Setup

To build the plugin:

```bash
gradle assemble
```
This will produce a zip file in `build/distributions`.

After building the zip file, you can install it like this

```bash
bin/elasticsearch-plugin install file:///path/to/ingest-tika-recursive/build/distribution/ingest-tika-recursive-0.0.1-SNAPSHOT.zip
```

## Bugs & TODO

* Unlike `ingest-attachment`, this runs the full set of Tika parsers, and with full java permissions.   It is thus more unsafe to run on untrusted data, particularly if elasticsearch itself is running in a priviledged environment.


