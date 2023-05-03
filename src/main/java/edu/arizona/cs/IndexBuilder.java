package edu.arizona.cs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

public class IndexBuilder {

    public static void main(String[] args) {

        String path = "/Users/sgrim/Desktop/483_final_project";
        IndexBuilder build = new IndexBuilder();
        // String path = "/Users/lilbig/Desktop/483_final_project";
        build.buildIndex(path);

    }

    /**
     * Builds the index by going through each line of the input text file, and
     * creating a document from each. The first bit of text is the docid which
     * ends once the first space character is seen, and everything afterwards
     * in the text contained in that document.
     */
    private void buildIndex(String directory) {
        System.out.println("********Index creation starting.");
        // Get file from resources folder
        // ClassLoader classLoader = getClass().getClassLoader();
        // File file = new File(classLoader.getResource(inputFilePath).getFile());
        File dir = new File(directory + "/wiki-subset-20140602-shortened");
        String[] dirContentTemp = dir.list();
        List<String> dirContent = Arrays.asList(dirContentTemp);

        int totalDocs = 0;
        try {
            Path path = Paths.get("src/main/java/edu/arizona/cs/");

            Analyzer analyzer = new CustomAnalyzer();

            FSDirectory index = FSDirectory.open(path);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            LMDirichletSimilarity similar = new LMDirichletSimilarity();
            config.setSimilarity(similar);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter w = new IndexWriter(index, config);
            w.deleteAll();

            for (String wikiFileName : dirContent) {
                String wikiFilePath = dir.getAbsolutePath() + "/" + wikiFileName;
                try (Scanner inputScanner = new Scanner(new File(wikiFilePath))) {

                    // Adds each line of the input file to the index as a new document
                    String line;
                    int numDocs = 0;
                    while (inputScanner.hasNextLine()) {
                        line = inputScanner.nextLine();
                        Document doc = new Document();

                        String text = "";
                        if (line.startsWith("[[")) {
                            doc.add(new StringField("docName", line.substring(2, line.length() - 2), Field.Store.YES));
                            while (inputScanner.hasNextLine()) {
                                line = inputScanner.nextLine();
                                if (line.equals("End of paragraph.[]")) {
                                    numDocs += 1;
                                    break;
                                } else {
                                    text += line;
                                }
                            }
                        }
                        doc.add(new TextField("text", text, Field.Store.YES));
                        w.addDocument(doc);
                    }
                    System.out.println("Created " + numDocs + " documents in the index.\n");
                    totalDocs += numDocs;
                    inputScanner.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            w.close();
        } catch (Exception e) {
        }
        System.out.println("Total documents created: " + totalDocs);
    }

    public class CustomAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            // Use the StandardTokenizer and apply the following filters:
            // LowerCaseFilter, EnglishPossessiveFilter, PorterStemFilter, and CustomFilter
            Tokenizer tokenizer = new StandardTokenizer();
            TokenStream filter = new LowerCaseFilter(tokenizer);
            filter = new EnglishPossessiveFilter(filter);
            filter = new PorterStemFilter(filter);
            // filter = new StopFilter(filter, EnglishAnalyzer.getDefaultStopSet());
            filter = new CustomFilter(filter);
            return new TokenStreamComponents(tokenizer, filter);
        }

        // Custom TokenFilter that removes tokens matching a URL pattern
        private class CustomFilter extends TokenFilter {
            private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
            private final Pattern pattern = Pattern.compile("^url=https?://.+");

            public CustomFilter(TokenStream input) {
                super(input);
            }

            @Override
            public boolean incrementToken() throws IOException {
                while (input.incrementToken()) {
                    String token = termAttr.toString();
                    if (!pattern.matcher(token).matches()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}