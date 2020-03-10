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
