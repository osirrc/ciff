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
For reference, we provide exports from the [Robust04](https://github.com/castorini/anserini/blob/master/docs/regressions-robust04.md) and [ClueWeb12-B13](https://github.com/castorini/anserini/blob/master/docs/regressions-cw12b13.md) collections:

| Collection | Configuration | Size | MD5 | Download |
|:-----------|:--------------|-----:|-----|:---------|
| Robust04   | CIFF export, complete | 162M | `01ce3b9ebfd664b48ffad072fbcae076` | [[Dropbox]](https://www.dropbox.com/s/rph6udiqs2k7bfo/robust04-complete-20200306.ciff.gz?dl=0) |
| Robust04   | CIFF export, queries only | 16M | `0a8ea07b6a262639e44ec959c4f53d44` | [[Dropbox]](https://www.dropbox.com/s/02i308p4fe2bqh6/robust04-queries-20200306.ciff.gz?dl=0) | [[Dropbox]]
| Robust04   | Source Lucene index | 135M | `b993045adb24bcbe292d6ed73d5d47b6` | [[Dropbox]](https://www.dropbox.com/s/omh95m1pe5gwhaj/lucene-index-ciff.robust04.20200306.tar.gz?dl=0)
| ClueWeb12-B13   | CIFF export, complete | 25G | `8fff3a57b9625eca94a286a61062ac82` | [[Dropbox]](https://www.dropbox.com/s/nbxpieqqp5z737h/cw12b-complete-20200309.ciff.gz?dl=0)
| ClueWeb12-B13   | CIFF export, queries only | 1.2G | `45063400bd5823b7f7fec2bc5cbb2d36` | [[Dropbox]](https://www.dropbox.com/s/bx82uwx2mdzm8jy/cw12b-queries-20200309.ciff.gz?dl=0)
| ClueWeb12-B13 | Source Lucene index |21G | `6ad327c9c837787f7d9508462e5aa822` | [[Dropbox]](https://www.dropbox.com/s/33lnfrbvr88b999/lucene-index-ciff.cw12b.20200309.tar.gz?dl=0)

We provide a full guide on how to replicate the above results [here](anserini-export-guide.md).

## CIFF Importers

A CIFF export can be ingested into a number of different search systems.

+ [PISA](https://github.com/pisa-engine/pisa) via the [PISA CIFF Binaries](https://github.com/pisa-engine/ciff) (includes Python and Rust versions).


