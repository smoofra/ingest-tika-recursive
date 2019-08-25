# Elasticsearch tika-recursive Ingest Processor

Explain the use case of this processor in a TLDR fashion.

## Usage


```
PUT _ingest/pipeline/tika-recursive-pipeline
{
  "description": "A pipeline to do whatever",
  "processors": [
    {
      "tika_recursive" : {
        "field" : "my_field"
      }
    }
  ]
}

PUT /my-index/my-type/1?pipeline=tika-recursive-pipeline
{
  "my_field" : "Some content"
}

GET /my-index/my-type/1
{
  "my_field" : "Some content"
  "potentially_enriched_field": "potentially_enriched_value"
}
```

## Configuration

| Parameter | Use |
| --- | --- |
| some.setting   | Configure x |
| other.setting  | Configure y |

## Setup

In order to install this plugin, you need to create a zip distribution first by running

```bash
gradle clean check
```

This will produce a zip file in `build/distributions`.

After building the zip file, you can install it like this

```bash
bin/elasticsearch-plugin install file:///path/to/ingest-tika-recursive/build/distribution/ingest-tika-recursive-0.0.1-SNAPSHOT.zip
```

## Bugs & TODO

* There are always bugs
* and todos...

