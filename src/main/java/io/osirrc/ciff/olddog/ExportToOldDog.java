package io.osirrc.ciff.olddog;

import io.osirrc.ciff.CommonIndexFileFormat;
import org.kohsuke.args4j.*;

import java.io.*;

public class ExportToOldDog {
  public static class Args {
    @Option(name = "-postings", metaVar = "[file]", required = true, usage = "postings file")
    String postings = "";

    @Option(name = "-dict", required = true, usage = "Filename for output dict csv file")
    String dict = "";

    // Only include terms if you want to create a csv file for this.
    @Option(name = "-terms", usage = "Filename for output terms csv file")
    String terms = "";

    // Creating the docs file is not necessary as the docs.txt in CIFF is the same.
    // If you want to create one anyway using a different separator and ordering you can use this parameter.
    @Option(name = "-docs", depends = {"-CIFFdocs"}, usage = "Filename for output docs csv file")
    String docs = "";

    @Option(name = "-CIFFdocs", metaVar = "[file]", depends = {"-docs"}, usage = "Filename of the CIFF docs file. ")
    String ciffDocs = "";
  }

  public static void main(String[] argv) throws Exception {
    ExportToOldDog.Args args = new ExportToOldDog.Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: ExportToOldDog " + parser.printExample(OptionHandlerFilter.REQUIRED));
      return;
    }

    if (!args.docs.equals("")) {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args.ciffDocs)));
      BufferedWriter docsWriter = new BufferedWriter(new FileWriter(args.docs));
      String line;
      while ((line = br.readLine()) != null) {
        String[] elements = line.split("\t");
        docsWriter.write(elements[1] + '|' + elements[0] + '|' + elements[2]);
        docsWriter.newLine();
      }
      docsWriter.flush();
      docsWriter.close();
    }

    BufferedWriter dictWriter = new BufferedWriter(new FileWriter(args.dict));

    BufferedWriter termsWriter = null;
    if (!args.terms.equals("")) {
      termsWriter = new BufferedWriter(new FileWriter(args.terms));
    }
    FileInputStream fileIn = new FileInputStream(args.postings);

    long termID = 0;
    while (true) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl == null) {
        break;
      }
      dictWriter.write(Long.toString(termID) + '|' + pl.getTerm() + '|' + pl.getDf());
      dictWriter.newLine();
      if (termsWriter != null) {
        long docID = 0;
        for (int j=0; j< pl.getDf(); j++) {
          docID += pl.getPostings(j).getDocid();
          termsWriter.write(Long.toString(termID) + '|' + docID + '|' + Long.toString(pl.getPostings(j).getTf()));
          termsWriter.newLine();
        }
      }
      termID += 1;
    }
    dictWriter.flush();
    dictWriter.close();
    if (termsWriter != null) {
      termsWriter.flush();
      termsWriter.close();
    }
    fileIn.close();
  }
}
