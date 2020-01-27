package io.osirrc.ciff.olddog;

import io.osirrc.ciff.CommonIndexFileFormat;
import org.kohsuke.args4j.*;

import java.io.*;

public class CIFFToCsv {
  public static class Args {
    @Option(name = "-postings", metaVar = "[file]", required = true, usage = "postings file")
    String postings = "";

    @Option(name = "-dict", required = true, usage = "Filename for output dict csv file")
    String dict = "";

    @Option(name = "-terms", required = true, usage = "Filename for output terms csv file")
    String terms = "";

    // Creating the docs file is not necessary as the docs.txt is the same. If you want to create one anyway using
    // a different separator and ordering you can use this parameter.
    @Option(name = "-docs", depends = {"-CIFFdocs"}, usage = "Filename for output docs csv file")
    String docs = "";

    @Option(name = "-CIFFdocs", metaVar = "[file]", depends = {"-docs"}, usage = "Filename of the CIFF docs file. ")
    String ciffDocs = "";
  }

  public static void main(String[] argv) throws Exception {
    CIFFToCsv.Args args = new CIFFToCsv.Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.err.println("Example: ReadCIFFToCsv " + parser.printExample(OptionHandlerFilter.REQUIRED));
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
    BufferedWriter termsWriter = new BufferedWriter(new FileWriter(args.terms));
    FileInputStream fileIn = new FileInputStream(args.postings);

    long termID = 0;
    while (true) {
      CommonIndexFileFormat.PostingsList pl = CommonIndexFileFormat.PostingsList.parseDelimitedFrom(fileIn);
      if (pl == null) {
        // We've read all postings...
        break;
      }
      dictWriter.write(Long.toString(termID) + '|' + pl.getTerm() + '|' + pl.getDf());
      dictWriter.newLine();
      long docID = 0;
      for (int j=0; j< pl.getDf(); j++) {
        docID += pl.getPosting(j).getDocid();
        termsWriter.write(Long.toString(termID) + '|' + docID + '|' + Long.toString(pl.getPosting(j).getTf()));
        termsWriter.newLine();
      }
      termID += 1;
    }
    dictWriter.flush();
    termsWriter.flush();
    dictWriter.close();
    termsWriter.close();
    fileIn.close();
  }
}
