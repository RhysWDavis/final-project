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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

// @SuppressWarnings("unused")
public class QueryEngine {
    boolean indexExists = false;
    String inputDirPath = "";
    Directory index;
    Analyzer analyzer = new StandardAnalyzer();

    public static void main(String[] args ) {
        try {
            String path = "/Users/lilbig/Desktop/483_final_project";

            
            //String whole_q = "The dominant paper in our nation's capital, it's among the top 10 U.S. papers in circulation";
            String whole_q = "This woman who won consecutive heptathlons at the Olympics went to UCLA on a basketball scholarship";
            String[] test_query = whole_q.split(" ");

            // String[] test_query = {"standards", "produced"};
            QueryEngine objQueryEngine = new QueryEngine(path);

            // change this to be a for loop over all queries in the 
            List<ResultClass> q = objQueryEngine.runQ1(test_query);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public QueryEngine(String filesDir){
        inputDirPath = filesDir;

        buildIndex();
    }

    /**
     * Builds the index by going through each line of the input text file, and
     * creating a document from each. The first bit of text is the docid which
     * ends once the first space character is seen, and everything afterwards
     * in the text contained in that document.
     */
    private void buildIndex() {
        System.out.println("********Index creation starting.");
        //Get file from resources folder
        // ClassLoader classLoader = getClass().getClassLoader();
        // File file = new File(classLoader.getResource(inputFilePath).getFile());
        File dir = new File(inputDirPath + "/wiki-subset-20140602-shortened");
        String[] dirContentTemp = dir.list();
        List<String> dirContent = Arrays.asList(dirContentTemp);
        
        for (String wikiFileName : dirContent) {
            String wikiFilePath = dir.getAbsolutePath() + "/" + wikiFileName;
            try (Scanner inputScanner = new Scanner(new File(wikiFilePath))) {
                Path path = Paths.get("src/main/java/edu/arizona/cs/");

                // analyzer = new StandardAnalyzer();
                index = FSDirectory.open(path);
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                IndexWriter w = new IndexWriter(index, config);
                w.deleteAll();

                // Adds each line of the input file to the index as a new document
                String line;
                int numDocs = 0;
                while (inputScanner.hasNextLine()) {
                    line = inputScanner.nextLine();
                    Document doc = new Document();

                    String text = "";
                    if (line.startsWith("[[")) {
                        doc.add(new StringField("docName", line.substring(2, line.length()-2), Field.Store.YES));
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
                w.close();
                inputScanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        indexExists = true;
    }

    public List<ResultClass> runQ1(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1 -----");
        String fullQuery = "";
        // runs the query
        for (String s : query) {
            fullQuery += s + " OR ";
        }
        fullQuery = fullQuery.substring(0, fullQuery.length()-3);
        

        return runQueries(fullQuery);
    }


    /**
     * Performs the functionality of giving a query to the index, obtaining the results,
     * and printing out and returning some of them.
     * 
     * @param fullQuery - The exact query to be passed to the index
     * @return a list of ResultClass objects of each document matching the fullQuery
     */
    public List<ResultClass> runQueries(String fullQuery) {
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
            for (int i = 0; i < hits.length; ++i) {
                int docID = hits[i].doc;
                Document d = searcher.doc(docID);

                ResultClass result = new ResultClass();
                result.DocName = d;
                result.docScore = hits[i].score;
                ans.add(result);
                // System.out.println("The document: " + result.DocName.get("docName") + " had a score of: " + result.docScore);
            }

            // Sort the result documents and print out the top k
            int k = 20;
            Collections.sort(ans);
            for (int i = 0; i < k; i++) {
                int numDocs = ans.size() - 1;
                ResultClass result = ans.get(numDocs - i);
                System.out.println("The document: " + result.DocName.get("docName") + " had a score of: " + result.docScore);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
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
