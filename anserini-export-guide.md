# Anserini Export Guide

For replicability, we provide detailed instructions for generating the exports on the main page.

## Robust04

The source index was constructed with the following command, based on v0.7.2 (as tagged in the repo):

```bash
sh target/appassembler/bin/IndexCollection -collection ClueWeb12Collection \
 -input /tuna1/collections/web/ClueWeb12-B13/ -index lucene-index-ciff.cw12b.20200309 \
 -generator JsoupGenerator -threads 8 -optimize
 ```

Note that `-optimize` must be specified to merge the index down to a single segment.

The follow two invocations created the exports:

```bash
target/appassembler/bin/ExportAnseriniLuceneIndex -output robust04-complete-20200306.ciff \
 -index lucene-index-ciff.robust04.20200306 -description "Anserini v0.7.2, Robust04 regression"

target/appassembler/bin/ExportAnseriniLuceneIndex -index lucene-index-ciff.robust04.20200306 \
 -output robust04-queries-20200306.ciff -termsFile src/main/resources/robust04-tokens.lucene-analyzed.txt \
 -description "Anserini v0.7.2, Robust04 regression"
```

## ClueWeb12-B13

The source index was constructed with the following command, based on v0.7.2 (as tagged in the repo):

```bash
git checkout anserini-0.7.2

sh target/appassembler/bin/IndexCollection -collection ClueWeb12Collection \
 -input /tuna1/collections/web/ClueWeb12-B13/ -index lucene-index-ciff.cw12b.20200309 \
 -generator JsoupGenerator -threads 8 -optimize
```

Note that `-optimize` must be specified to merge the index down to a single segment.

The follow two invocations created the exports:

```bash
target/appassembler/bin/ExportAnseriniLuceneIndex -output cw12b-complete-20200309.ciff.gz
 -index lucene-index-ciff.cw12b.20200309 -description "Anserini v0.7.2, ClueWeb12-B13 regression"

target/appassembler/bin/ExportAnseriniLuceneIndex -output cw12b-queries-20200309.ciff.gz \
 -index lucene-index-ciff.cw12b.20200309 -description "Anserini v0.7.2, ClueWeb12-B13 regression" \
 -termsFile src/main/resources/cw12b-tokens.lucene-analyzed.txt \
```

On a modern desktop with an SSD, the complete export takes a bit less than 3 hours to complete.
The export with queries only takes about 10 minutes.
