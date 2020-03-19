/*
 * CIFF (Common Index File Format):
 * an open, binary exchange format for index structures to support search engine interoperability
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.osirrc.ciff;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ReadCIFF {
  public static class Args {
    @Option(name = "-input", metaVar = "[file]", required = true, usage = "postings file")
    public String input = "";

    @Option(name = "-dumpInterval", metaVar = "[num]",
        usage = "prints PostingsList and DocRecord messages at this regular inverval for debugging purposes")
    public int dumpInterval = 100000;
  }

  public static void main(String[] argv) throws Exception {
    Args args = new Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: ReadCIFF " + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    InputStream fileIn;

    if (args.input.endsWith(".gz")) {
      fileIn = new GZIPInputStream(new FileInputStream(args.input));
    } else {
      fileIn = new FileInputStream(args.input);
    }

    System.out.println("Reading header...");
    CommonIndexFileFormat.Header header = CommonIndexFileFormat.Header.parseDelimitedFrom(fileIn);

    System.out.println("=== Header === ");
    System.out.println(String.format("version: %,d", header.getVersion()));
    System.out.println(String.format("num_postings_lists: %,d", header.getNumPostingsLists()));
    System.out.println(String.format("num_doc_records: %,d", header.getNumDocs()));
    System.out.println(String.format("total_postings_lists: %,d", header.getTotalPostingsLists()));
    System.out.println(String.format("total_docs: %,d", header.getTotalDocs()));
    System.out.println(String.format("total_terms_in_collection: %,d", header.getTotalTermsInCollection()));
    System.out.println(String.format("average_doclength: %f", header.getAverageDoclength()));
    System.out.println(String.format("description: %s", header.getDescription()));
    System.out.println();

    System.out.println(String.format("Expecting %,d PostingsList and %,d DocRecords in this export.\n",
        header.getNumPostingsLists(), header.getNumDocs()));

    System.out.println(String.format("Reading every PostingsList, dumping out every %,dth:", args.dumpInterval));
    long sumOfAllTfs = 0;
    for (int i=0; i<header.getNumPostingsLists(); i++ ) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl.getDf() != pl.getPostingsCount()) {
        throw new RuntimeException(String.format(
            "Unexpected number of postings! expected %,d got %,d", pl.getDf(), pl.getPostingsCount()));
      }

      if (i % args.dumpInterval == 0) {
        System.out.print(String.format("term: '%s', df=%,d, cf=%,d", pl.getTerm(), pl.getDf(), pl.getCf()));

        for (int j = 0; j < (pl.getDf() > 10 ? 10 : pl.getDf()); j++) {
          System.out.print(String.format(" (%d, %d)", pl.getPostings(j).getDocid(), pl.getPostings(j).getTf()));
        }
        System.out.println(pl.getDf() > 10 ? " ..." : "");
      }

      for (int j=0; j<pl.getDf(); j++) {
        sumOfAllTfs += pl.getPostings(j).getTf();
      }
    }
    System.out.println(String.format("%,d postings lists read\n", header.getNumPostingsLists()));

    System.out.println(String.format("Reading every DocRecord, dumping out every %,dth:", args.dumpInterval));
    long totalTermsInCollection = 0; // Should be sum of doclengths.
    for (int i=0; i<header.getNumDocs(); i++) {
      CommonIndexFileFormat.DocRecord docRecord = CommonIndexFileFormat.DocRecord.parseDelimitedFrom(fileIn);
      if (i % args.dumpInterval == 0) {
        System.out.println(String.format(
            "%d\t%s\t%d", docRecord.getDocid(), docRecord.getCollectionDocid(), docRecord.getDoclength()));
      }
      totalTermsInCollection += docRecord.getDoclength();
    }

    System.out.println("\nIntegrity checks:");
    System.out.print(" 'total_terms_in_collection' in Header == sum of doclengths from DocRecords: ");
    if (totalTermsInCollection == header.getTotalTermsInCollection()) {
      System.out.print("[PASSED]\n");
    } else {
      System.out.print(String.format("[FAILED] %,d vs. %,d\n",
          header.getTotalTermsInCollection(), totalTermsInCollection));
    }
    System.out.print(" 'total_terms_in_collection' in Header == sum of all tfs in all Postings:    ");
    if (sumOfAllTfs == header.getTotalTermsInCollection()) {
      System.out.print("[PASSED]\n");
    } else {
      System.out.print(String.format("[FAILED] %,d vs. %,d\n",
          header.getTotalTermsInCollection(), totalTermsInCollection));
    }

    fileIn.close();
  }
}
