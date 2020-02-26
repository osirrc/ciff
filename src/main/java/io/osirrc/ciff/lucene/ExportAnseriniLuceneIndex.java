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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.osirrc.ciff.CommonIndexFileFormat.DocRecord;
import static io.osirrc.ciff.CommonIndexFileFormat.Header;
import static io.osirrc.ciff.CommonIndexFileFormat.Posting;
import static io.osirrc.ciff.CommonIndexFileFormat.PostingsList;
import static io.osirrc.ciff.CommonIndexFileFormat.PostingsListOrBuilder;

public class ExportAnseriniLuceneIndex {
  public static class Args {
    @Option(name = "-output", metaVar = "[file]", required = true, usage = "postings output")
    public String output = "";

    @Option(name = "-index", metaVar = "[path]", required = true, usage = "index path")
    public String index = "";

    @Option(name = "-termsFile", metaVar = "[file]", usage = "file containing terms to dump")
    public String termsFile = null;

    @Option(name = "-contentsField", metaVar = "[field name]", usage = "name of the 'contents' field")
    public String contentsField = "contents";

    @Option(name = "-docidField", metaVar = "[field name]", usage = "name of the 'docid' field")
    public String docidsField = "id";
  }

  public static Counts countPostingsLists(IndexReader reader, String field, Set<String> terms) throws IOException {
    Counts counts = new Counts();

    LeafReader leafReader = reader.leaves().get(0).reader();
    TermsEnum termsEnum = leafReader.terms(field).iterator();
    BytesRef bytesRef = termsEnum.next();
    while (bytesRef != null) {
      // This is the current term in the dictionary.
      String token = bytesRef.utf8ToString();
      counts.total++;

      if (terms != null && !terms.contains(token)) {
        bytesRef = termsEnum.next();
        continue;
      }

      bytesRef = termsEnum.next();
      counts.export++;
    }

    return counts;
  }

  public static class Counts {
    int export = 0;
    int total = 0;
  }

  public static void main(String[] argv) throws Exception {
    Args args = new Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: ExportAnseriniLuceneIndex " + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    // If specified, load the query terms
    Set<String> terms = null;
    if (args.termsFile != null) {
      terms = new HashSet<>();
      BufferedReader reader = new BufferedReader(new FileReader(args.termsFile));
      String line = reader.readLine();
      while (line != null) {
        line = line.strip();
        terms.add(line);
        line = reader.readLine();
      }
      reader.close();
      System.out.println(String.format("Loaded termsFile, only dumping postings for %d specified terms.", terms.size()));
    }

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(args.index)));

    if (reader.leaves().size() != 1) {
      throw new RuntimeException("There should be only one leaf, index the collection using the -optimize flag");
    }

    FileOutputStream fileOut = new FileOutputStream(args.output);

    // Go through the index once to count the number of postings lists we're going to export and the vocab size.
    // We need this value to populate the header.
    System.out.println("Taking a pass through the index to count the number of postings in the index...");
    Counts counts = countPostingsLists(reader, args.contentsField, terms);
    System.out.println(String.format("Exporting %d postings lists out of %d total",
        counts.export, counts.total));

    // Write the header.
    System.out.println("Writing the header...");
    Header.newBuilder()
        .setVersion(1)
        .setNumPostingsLists(counts.export)
        .setNumDocRecords(reader.maxDoc())   // We're exporting all docs.
        .setTotalPostingsList(counts.total)
        .setTotalDocs(reader.maxDoc())
        .setTotalTermsInCollection(reader.getSumTotalTermFreq(args.contentsField))
        .setAverageDoclength((double) reader.getSumTotalTermFreq(args.contentsField) / reader.maxDoc())
        .setDescription("Index built by Anserini.")
        .build().writeDelimitedTo(fileOut);

    // Now we can write the postings lists.
    int cnt = 0;
    LeafReader leafReader = reader.leaves().get(0).reader();
    TermsEnum termsEnum = leafReader.terms(args.contentsField).iterator();
    BytesRef bytesRef = termsEnum.next();
    while (bytesRef != null) {
      // This is the current term in the dictionary.
      String token = bytesRef.utf8ToString();

      if (terms != null && !terms.contains(token)) {
        bytesRef = termsEnum.next();
        continue;
      }

      Term term = new Term("contents", token);
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

        ((PostingsList.Builder) plBuilder).addPostings(
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
      if (cnt % 100000 == 0) {
        System.out.println("Wrote " + cnt + " postings lists...");
      }
    }
    System.out.println("Total of " + cnt + " postings lists written.");

    // Read the doclengths (norms) into memory
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

    // Write the doc records: (docid, collection docid, doclength)
    System.out.println("Writing doc records...");
    for (int i=0; i<reader.maxDoc(); i++) {
      if (!normsMap.containsKey(i)) {
        throw new Exception(String.format("Norm doesn't exist for docid %d!", i));
      }
      DocRecord.newBuilder()
          .setDocid(i)
          .setCollectionDocid(reader.document(i).getField(args.docidsField).stringValue())
          .setDoclength(normsMap.get(i))
          .build().writeDelimitedTo(fileOut);

      if (i % 100000 == 0 && i != 0) {
        System.out.println("Wrote " + i + " doc records...");
      }
    }
    System.out.println("Done!");

    fileOut.close();
    reader.close();
  }
}
