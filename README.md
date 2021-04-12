# Common Index File Format

The Common Index File Format (CIFF) represents an attempt to build a binary data exchange format for open-source search engines to interoperate by sharing index structures.
For more details, check out:

+ Jimmy Lin, Joel Mackenzie, Chris Kamphuis, Craig Macdonald, Antonio Mallia, Michał Siedlaczek, Andrew Trotman, Arjen de Vries. [Supporting Interoperability Between Open-Source Search Engines with the Common Index File Format](https://arxiv.org/abs/2003.08276). _arXiv:2003.08276_.

All data are contained in a single file, with the extension `.ciff`.
The file comprises a sequence of [delimited](https://developers.google.com/protocol-buffers/docs/techniques) protobuf messages defined [here](src/main/protobuf/CommonIndexFileFormat.proto), exactly as follows:

+ A `Header`
+ Exactly the number of `PostingsList` messages specified in the `num_postings_lists` field of the `Header`
+ Exactly the number of `DocRecord` messages specified in the `num_docs` field of the `Header`

See [our design rationale](design-rationale.md) for additional discussion.

Explained in terms of xkcd, we're trying to avoid [this](https://xkcd.com/927/).
Instead, CIFF aims to be [this](https://xkcd.com/1406/).

## Getting Started

After cloning this repo, build CIFF with Maven:

```
mvn clean package appassembler:assemble
```

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

The follow invocation can be used to examine an export:

```bash
target/appassembler/bin/ReadCIFF -input robust04-complete-20200306.ciff.gz
```

We provide a full guide on how to replicate the above results [here](anserini-export-guide.md).

## CIFF Importers

A CIFF export can be ingested into a number of different search systems.

+ [JASSv2](https://github.com/andrewtrotman/JASSv2) via the tool [ciff_to_JASS](https://github.com/andrewtrotman/JASSv2/tree/master/tools).
+ [PISA](https://github.com/pisa-engine/pisa) via the [PISA CIFF Binaries](https://github.com/pisa-engine/ciff).
+ [OldDog](https://github.com/chriskamphuis/olddog) by [creating csv files through CIFF](https://github.com/Chriskamphuis/olddog/blob/master/src/main/java/nl/ru/convert/CiffToCsv.java)
+ [Terrier](http://terrier.org) via the [Terrier-CIFF plugin](https://github.com/terrierteam/terrier-ciff)

##  Tips for writing your own CIFF Importer / Exporter

The systems above all provide concrete examples of taking an existing CIFF structure and converting it into a different (internal) index format.
Most of the data/structures within the CIFF are quite straightforward and self-documenting. However, there are a few important details which
should be noted.

1. The default CIFF exports come from Anserini. Those exports are engineered to encode document identifiers *as deltas (d-gaps).* Hence, when decoding a CIFF structure, care needs to be taken to recover the original identifiers by computing a prefix sum across each postings list. See the discussion [here](https://github.com/osirrc/ciff/issues/19).

2. Since Anserini is based on Lucene, it is important to note that document lengths are encoded in a lossy manner. This means that the document lengths recorded in the `DocRecord` structure are *approximate* - see the discussion [here](https://github.com/osirrc/ciff/issues/21).

3. Multiple records are stored in a single file using Java protobuf's parseDelimitedFrom() and writeDelimitedTo() methods. Unfortunately, these methods are not available in the bindings for other languages. These can be trivially reimplemented be reading/writing the bytesize of the record using varint - see the discussion [here](https://github.com/osirrc/ciff/issues/27).
