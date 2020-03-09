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

## Reference Lucene Indexes

Currently, this repo provides an utility to export CIFF from Lucene, via [Anserini](http://anserini.io/).
For reference, we provide exports from the [Robust04](https://github.com/castorini/anserini/blob/master/docs/regressions-robust04.md) collection:

+ [`robust04-complete-20200306.ciff.gz`](https://www.dropbox.com/s/rph6udiqs2k7bfo/robust04-complete-20200306.ciff.gz?dl=0) (162M): complete index
+ [`robust04-queries-20200306.ciff.gz`](https://www.dropbox.com/s/02i308p4fe2bqh6/robust04-queries-20200306.ciff.gz?dl=0) (16M): postings for [query terms](src/main/resources/robust04-tokens.lucene-analyzed.txt) only
+ [`lucene-index-ciff.robust04.20200306.tar.gz`](https://www.dropbox.com/s/omh95m1pe5gwhaj/lucene-index-ciff.robust04.20200306.tar.gz?dl=0) (171M): raw Lucene index (source of above exports)

The follow invocation can be used to examine an export:

```bash
target/appassembler/bin/ReadCIFF -input robust04-complete-20200306.ciff.gz
```

For replicability, the source index was constructed with the following command, based on v0.7.2 (as tagged in the repo):

```bash
sh target/appassembler/bin/IndexCollection -collection ClueWeb12Collection \
 -input /tuna1/collections/web/ClueWeb12-B13/ -index lucene-index-ciff.cw12b.20200309 \
 -generator JsoupGenerator -threads 8 -optimize
 ```

Note that `-optimize` must be specified to merge the index down to a single segment.

The follow two invocations created the above exports:

```bash
target/appassembler/bin/ExportAnseriniLuceneIndex -output robust04-complete-20200306.ciff \
 -index lucene-index-ciff.robust04.20200306 -description "Anserini v0.7.2, Robust04 regression"

target/appassembler/bin/ExportAnseriniLuceneIndex -index lucene-index-ciff.robust04.20200306 \
 -output robust04-queries-20200306.ciff -termsFile src/main/resources/robust04-tokens.lucene-analyzed.txt \
 -description "Anserini v0.7.2, Robust04 regression"
```

## Ingestion Pipelines

Once we have exported a Lucene index, it can be ingested into a number of different search systems.


+ [PISA](https://github.com/pisa-engine/pisa) via the [PISA CIFF Binaries](https://github.com/pisa-engine/ciff) (includes Python and Rust versions).


