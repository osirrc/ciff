package io.osirrc.ciff.olddog;

import io.osirrc.ciff.CommonIndexFileFormat;
import org.kohsuke.args4j.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ExportTermsToOldDogBulk {
  /**
   * Create the columns for MonetDB directly instead of creating an inbetween csv. This does not
   * work for the other tables as they can contain utf-8 characters. These have to be loaded through
   * the csv loader.
   */

  public static class Args {
    @Option(name = "-postings", metaVar = "[file]", required = true, usage = "postings file")
    String postings = "";

    @Option(name = "-termidFile", required = true, usage = "Filename for termid column data")
    String termIDFile = "";

    @Option(name = "-docidFile", required = true, usage = "Filename for docid column data")
    String docIDFile = "";

    @Option(name = "-countFile", required = true, usage = "Filename for count column data")
    String countFile = "";
  }

  public static void main(String[] argv) throws Exception {
    ExportTermsToOldDogBulk.Args args = new ExportTermsToOldDogBulk.Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: ExportTermsToOldDogBulk " + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    FilterOutputStream termIDWriter = new FilterOutputStream(new BufferedOutputStream(new FileOutputStream(args.termIDFile)));
    FilterOutputStream docIDWriter = new FilterOutputStream(new BufferedOutputStream(new FileOutputStream(args.docIDFile)));
    FilterOutputStream countWriter = new FilterOutputStream(new BufferedOutputStream(new FileOutputStream(args.countFile)));

    BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(args.postings));
    int termID = 0;
    ByteBuffer buffer = null;
    while (true) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl == null) {
        break;
      }
      int docID = 0;
      for (int j=0; j< pl.getDf(); j++) {
        docID += pl.getPosting(j).getDocid();

        buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(termID);
        termIDWriter.write(buffer.array());

        buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(docID);
        docIDWriter.write(buffer.array());

        buffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        buffer.putInt(pl.getPosting(j).getTf());
        countWriter.write(buffer.array());
      }
      termID += 1;
      termIDWriter.flush();
      docIDWriter.flush();
      countWriter.flush();
    }
    termIDWriter.close();
    docIDWriter.close();
    countWriter.close();
  }
}
