package io.osirrc.ciff.lucene;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

import static io.osirrc.ciff.CommonIndexFileFormat.Posting;
import static io.osirrc.ciff.CommonIndexFileFormat.PostingsList;
import static io.osirrc.ciff.CommonIndexFileFormat.PostingsListOrBuilder;

public class DumpLuceneIndex {
  public static class Args {
    @Option(name = "-postingsOutput", metaVar = "[file]", required = true, usage = "postings output")
    public String postingsOutput = "";

    @Option(name = "-docsOutput", metaVar = "[file]", required = true, usage = "docid output")
    public String docsOutput = "";

    @Option(name = "-index", metaVar = "[path]", required = true, usage = "index path")
    public String index = "";

    @Option(name = "-max", metaVar = "[int]", usage = "maximum number of postings to write")
    public int max = Integer.MAX_VALUE;

    @Option(name = "-contentsField", metaVar = "[field name]", usage = "name of the 'contents' field")
    public String contentsField = "contents";

    @Option(name = "-docidField", metaVar = "[field name]", usage = "name of the 'docid' field")
    public String docidsField = "id";
  }

  public static void main(String[] argv) throws Exception {
    Args args = new Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: DumpLuceneIndex " + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(args.index)));

    System.out.println("Writing postings...");
    FileOutputStream fileOut = new FileOutputStream(args.postingsOutput);
    int cnt = 0;
    // This is how you iterate through terms in the postings list.
    LeafReader leafReader = reader.leaves().get(0).reader();
    TermsEnum termsEnum = leafReader.terms(args.contentsField).iterator();
    BytesRef bytesRef = termsEnum.next();
    while (bytesRef != null) {
      // This is the current term in the dictionary.
      String token = bytesRef.utf8ToString();
      Term term = new Term("contents", token);
      //System.out.print(token + " (df = " + reader.docFreq(term) + "):");

      long df = reader.docFreq(term);
      long cf = reader.totalTermFreq(term);

      PostingsListOrBuilder plBuilder =
          PostingsList.newBuilder().setTerm(token).setDf(df).setCf(cf);

      int curDocid;
      int prevDocid = -1;
      int postingsWritten = 0;
      PostingsEnum postingsEnum = leafReader.postings(term);
      while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
        curDocid = postingsEnum.docID();
        // gap (i.e., delta) encoding.
        int code = prevDocid == -1 ? curDocid : curDocid - prevDocid;

        //System.out.print(String.format(" (%s, %s, %s)", postingsEnum.docID(), postingsEnum.freq(), code));
        ((PostingsList.Builder) plBuilder).addPosting(
            Posting.newBuilder().setDocid(code).setTf(postingsEnum.freq()).build());
        postingsWritten++;
        prevDocid = curDocid;
      }
      // The number of postings written should be the same as the df.
      if (postingsWritten != df) {
        throw new RuntimeException(String.format("Unexpected number of postings! expected %d got %d", df, postingsWritten));
      }

      PostingsList pl = ((PostingsList.Builder) plBuilder).build();
      pl.writeDelimitedTo(fileOut);

      bytesRef = termsEnum.next();

      cnt++;
      if ( cnt > args.max) {
        break;
      }
      if (cnt % 10000 == 0) {
        System.out.println("Wrote " + cnt + " postings...");
      }
    }
    System.out.println("Total of " + cnt + " postings written.");
    fileOut.close();

    Map<Integer, Integer> normsMap = new HashMap<>();
    System.out.println("Reading norms into memory...");
    for (LeafReaderContext context : reader.leaves()) {
      leafReader = context.reader();
      NumericDocValues docValues = leafReader.getNormValues(args.contentsField);
      while (docValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
        normsMap.put(docValues.docID() + context.docBase, SmallFloat.byte4ToInt((byte) docValues.longValue()));
      }
    }
    System.out.println("Done!");

    System.out.println("Writing docids...");
    FileOutputStream docsOut = new FileOutputStream(args.docsOutput);
    for (int i=0; i<reader.maxDoc(); i++) {
      if (!normsMap.containsKey(i)) {
        throw new Exception(String.format("Norm doesn't exist for docid %d!", i));
      }
      docsOut.write((i + "\t" + reader.document(i).getField(args.docidsField).stringValue() +
          "\t" + normsMap.get(i) + "\n").getBytes());
    }
    docsOut.close();
    System.out.println("Done!");

    reader.close();
  }
}
