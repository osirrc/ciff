package io.osirrc.ciff;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import java.io.FileInputStream;

public class ReadCIFF {
  public static class Args {
    @Option(name = "-input", metaVar = "[file]", required = true, usage = "postings file")
    public String input = "";
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

    FileInputStream fileIn = new FileInputStream(args.input);
    System.out.println("Reading header...");
    CommonIndexFileFormat.Header header = CommonIndexFileFormat.Header.parseDelimitedFrom(fileIn);

    System.out.println("=== Header === ");
    System.out.println(String.format("version: %d", header.getVersion()));
    System.out.println(String.format("num_postings_lists: %d", header.getNumPostingsLists()));
    System.out.println(String.format("num_doc_records: %d", header.getNumDocRecords()));
    System.out.println(String.format("total_postings_list: %d", header.getTotalPostingsList()));
    System.out.println(String.format("total_docs: %d", header.getTotalDocs()));
    System.out.println(String.format("total_terms_in_collection: %d", header.getTotalTermsInCollection()));
    System.out.println(String.format("average_doclength: %f", header.getAverageDoclength()));
    System.out.println(String.format("description: %s", header.getDescription()));
    System.out.println("");

    System.out.println(String.format("Expecting %d postings lists and %d doc records in this export.",
        header.getNumPostingsLists(), header.getNumDocRecords()));

    for (int i=0; i<header.getNumPostingsLists(); i++ ) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl.getDf() != pl.getPostingsCount()) {
        throw new RuntimeException(String.format(
            "Unexpected number of postings! expected %d got %d", pl.getDf(), pl.getPostingsCount()));
      }

      if (i % 100000 == 0) {
        System.out.print(String.format("term: '%s', df=%d, cf=%d", pl.getTerm(), pl.getDf(), pl.getCf()));

        for (int j = 0; j < (pl.getDf() > 10 ? 10 : pl.getDf()); j++) {
          System.out.print(String.format(" (%d, %d)", pl.getPostings(j).getDocid(), pl.getPostings(j).getTf()));
        }
        System.out.println(pl.getDf() > 10 ? " ..." : "");
      }
    }

    for (int i=0; i<header.getNumDocRecords(); i++) {
      CommonIndexFileFormat.DocRecord docRecord = CommonIndexFileFormat.DocRecord.parseDelimitedFrom(fileIn);
      if (i % 100000 == 0) {
        System.out.println(String.format("%d\t%s\t%d", docRecord.getDocid(), docRecord.getCollectionDocid(), docRecord.getDoclength()));
      }
    }
    fileIn.close();
  }
}
