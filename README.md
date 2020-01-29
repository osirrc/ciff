# Common Index File Format

The Common Index File Format (CIFF) represents an attempt to build a binary data exchange format for open-source search engines to interoperate by sharing index structures.

It currently comprises two components:

+ A [Protocol Buffer definition for encoding postings list](src/main/protobuf/CommonIndexFileFormat.proto).
+ A plain-text file for sharing document metadata. Currently, this is a TSV file with three fields: internal docid, collection docid, and document length. The internal docid refers to the docid used in the postings lists; the collection docid is the external identifier used in the document collection (i.e., those matching the qrels).

## Exporting Lucene Indexes

Currently, this repo provides utilities for exporting CIFF from Lucene, via [Anserini](http://anserini.io/).
First, build an Anserini index:

```bash
sh target/appassembler/bin/IndexCollection -collection TrecCollection \
 -input /tuna1/collections/newswire/disk45/ -index lucene-index.robust04.optimized \
 -generator JsoupGenerator -threads 16 -optimize
```

Note that `-optimize` must be specified to merge the index down to a single segment.

Here's the invocation for exporting the entire index, and a sample program to read the resulting postings back:

```bash
target/appassembler/bin/DumpLuceneIndex -index /path/to/index \
  -docsOutput robust04-docs.txt -postingsOutput robust04-postings.pb

target/appassembler/bin/ReadCIFF -postings robust04-postings.pb
```

Here's the invocation for exporting just the [query terms in Robust04](src/main/resources/robust04-tokens.lucene-analyzed.txt), and a sample program to read the resulting postings back:

```bash
target/appassembler/bin/DumpLuceneIndex -index /path/to/index \
  -docsOutput robust04-docs.txt -termsFile src/main/resources/robust04-tokens.lucene-analyzed.txt \
  -postingsOutput robust04-postings-queries-only.pb

target/appassembler/bin/ReadCIFF -postings robust04-postings-queries-only.pb -max 600
```

## Ingestion Pipelines

Once we have exported a Lucene index, it can be ingested into a number of different search systems.


+ [PISA](https://github.com/pisa-engine/pisa) via the [PISA CIFF Binaries](https://github.com/pisa-engine/common-index-format) (includes Python and Rust versions).


