package edu.arizona.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

// @SuppressWarnings("unused")
public class QueryEngine {
    boolean indexExists = false;
    String inputFilePath = "";
    Directory index;
    Analyzer analyzer;

    public QueryEngine(String inputFile){
        inputFilePath = inputFile;
        buildIndex();
    }

    /**
     * Builds the index by going through each line of the input text file, and
     * creating a document from each. The first bit of text is the docid which
     * ends once the first space character is seen, and everything afterwards
     * in the text contained in that document.
     */
    private void buildIndex() {
        //Get file from resources folder
        // ClassLoader classLoader = getClass().getClassLoader();
        // File file = new File(classLoader.getResource(inputFilePath).getFile());
        File file = new File(inputFilePath);
        
        try (Scanner inputScanner = new Scanner(file)) {
            Path path = Paths.get("src/main/java/edu/arizona/cs/");

            analyzer = new StandardAnalyzer();
            index = FSDirectory.open(path);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            w.deleteAll();

            // Adds each line of the input file to the index as a new document
            String line;
            while (inputScanner.hasNextLine()) {
                line = inputScanner.nextLine();
                Document doc = new Document();

                String text = "";
                if (line.startsWith("[[")) {
                    doc.add(new StringField("docName", line.substring(2, line.length()-2), Field.Store.YES));
                    while (inputScanner.hasNextLine()) {
                        line = inputScanner.nextLine();
                        if (line.equals("End of paragraph.[]\n")) {
                            break;
                        } else {
                            text += line;
                        }
                    }
                }
                doc.add(new TextField("text", text, Field.Store.YES));
                w.addDocument(doc);
            }
            
            w.close();
            inputScanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexExists = true;
    }

    public static void main(String[] args ) {
        try {
            String fileName = "/Users/lilbig/Desktop/wiki_temp_subset/small_last_paragraph.txt";
            System.out.println("********Index creation starting.");
            String[] test_query = {"the", "standards", "produced", "by", "BSI", "Group"};            // String[] test_query = {"the", "standards", "produced", "by", "BSI", "Group"};
            // String[] test_query = {"standards"};
            QueryEngine objQueryEngine = new QueryEngine(fileName);
            List<ResultClass> q = objQueryEngine.runQ1(test_query);
            for (ResultClass r : q) {
                System.out.println(r);
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Performs the functionality of giving a query to the index, obtaining the results,
     * and printing out and returning some of them.
     * 
     * @param fullQuery - The exact query to be passed to the index
     * @return a list of ResultClass objects of each document matching the fullQuery
     */
    public List<ResultClass> runQueries(String fullQuery) {
        if(!indexExists) {
            buildIndex();
        }
        List<ResultClass> ans = new ArrayList<ResultClass>();

        try {
            Query q = new QueryParser("text", analyzer).parse(fullQuery);

            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            int numHits = searcher.count(q);
            if (numHits == 0) {
                return null;
            }
            TopDocs docs = searcher.search(q, numHits);
            ScoreDoc[] hits = docs.scoreDocs;

            // For each document matching the query, add it to the list and print out some info
            for (int i=0; i<hits.length; ++i) {
                int docID = hits[i].doc;
                Document d = searcher.doc(docID);

                ResultClass result = new ResultClass();
                result.DocName = d;
                result.docScore = hits[i].score;
                ans.add(result);
                System.out.println("The document: " + result.DocName.get("docid") + " had a score of: " + result.docScore);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
    }

    public List<ResultClass> runQ1(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1 -----");
        String fullQuery = "";
        // runs the query
        for (String s : query) {
            fullQuery += s + " AND ";
        }
        fullQuery = fullQuery.substring(0, fullQuery.length()-4);
        

        return runQueries(fullQuery);
    }

    public List<ResultClass> oldQs(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1_1 -----");

        // runs the query: information retrieval
        String fullQuery = query[0] + " " + query[1];
        // runs the query: information AND retrieval
        fullQuery = query[0] + " AND " + query[1];
        // runs the query: information AND NOT retrieval
        fullQuery = query[0] + " NOT " + query[1];
        // runs the query: "information retrieval" (occurring with no words in between)
        fullQuery = "\"" + query[0] + " " + query[1] + "\"~1";

        return runQueries(fullQuery);
    }

}
