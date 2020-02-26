# Common Index File Format

The Common Index File Format (CIFF) represents an attempt to build a binary data exchange format for open-source search engines to interoperate by sharing index structures.

All data are contained in a single file, with the extension `.ciff`.
The file comprises a sequence of [delimited](https://developers.google.com/protocol-buffers/docs/techniques) protobuf messages defined [here](src/main/protobuf/CommonIndexFileFormat.proto), exactly as follows:

+ A `Header`
+ Exactly the number of `PostingsList` messages specified in the `num_postings_lists` field of the `Header`
+ Exactly the number of `DocRecord` messages specified in the `num_docs` field of the `Header`

_Design Rationale_: 
One might consider an alternative design comprising of a single protobuf message (e.g., the postings lists and doc records are contained in the header).
In this case, the entire specification of CIFF would be captured in a single protobuf definition.
This design was considered and rejected because it seems to run counter to [best practices suggested by Google](https://developers.google.com/protocol-buffers/docs/techniques): individual protobuf messages shouldn't be that large.
Furthermore, Google's automatically-generated code bindings appear to manipulate individual protobuf messages in memory, which would not be practical in our use case for large collections if the entire index were a single protobuf message.

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


+ [PISA](https://github.com/pisa-engine/pisa) via the [PISA CIFF Binaries](https://github.com/pisa-engine/ciff) (includes Python and Rust versions).


