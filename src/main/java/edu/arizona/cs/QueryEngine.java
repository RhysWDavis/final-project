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
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

// @SuppressWarnings("unused")
public class QueryEngine {
    private boolean indexExists = false;
    private String inputDirPath = "";
    private Directory index;
    private Analyzer analyzer = new StandardAnalyzer();
    private ArrayList<String[]> jQuestions;
    private static final int CAT_INDEX = 0; 
    private static final int Q_INDEX = 1; 
    private static final int ANS_INDEX = 2; 


    public static void main(String[] args ) {
        try {
            String path = "/Users/lilbig/Desktop/483_final_project";
            QueryEngine objQueryEngine = new QueryEngine(path);
            objQueryEngine.getJQuestions(path);

            // objQueryEngine.runQs();
            objQueryEngine.checkExistence();

            //String whole_q = "The dominant paper in our nation's capital, it's among the top 10 U.S. papers in circulation";
            // String whole_q = "This woman who won consecutive heptathlons at the Olympics went to UCLA on a basketball scholarship";
            // String whole_q = "One of the N.Y. Times' headlines on this landmark 1973 Supreme Court decision was \"Cardinals shocked\"";
            // String[] test_query = whole_q.split(" ");
            // List<ResultClass> q = objQueryEngine.runQ1(test_query);
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
        
        try {
            Path path = Paths.get("src/main/java/edu/arizona/cs/");

            // analyzer = new StandardAnalyzer();
            index = FSDirectory.open(path);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
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
                    inputScanner.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            w.close();
        } catch (Exception e) {}
        indexExists = true;
    }

    public void runQs() throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q -----");

        Scanner userInput = new Scanner(System.in);
        
        System.out.println("Please enter a query (or STOP)\n");
        String q = userInput.nextLine();

        while (!q.equals("STOP")) {
            String[] query = q.split(" ");

            String fullQuery = "";
            for (String s : query) {
                fullQuery += s + " OR ";
            }
            fullQuery = fullQuery.substring(0, fullQuery.length()-3);

            // System.out.println("You entered the query: " + fullQuery);
            runQueries(fullQuery);

            System.out.println("Please enter a query (or STOP)\n");
            q = userInput.nextLine();
        }
        userInput.close();
    }


    /**
     * Performs the functionality of giving a query to the index, obtaining the results,
     * and printing out and returning some of them.
     * 
     * @param fullQuery - The exact query to be passed to the index
     * @return a list of ResultClass objects of each document matching the fullQuery
     */
    public List<ResultClass> runQueries(String fullQuery, int numHits) {
        List<ResultClass> ans = new ArrayList<ResultClass>();

        try {
            Query q = new QueryParser("text", analyzer).parse(fullQuery);

            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            
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
                double score = Math.round(result.docScore * 1000d) / 1000d;
                System.out.println("The document: " + result.DocName.get("docName") + " had a score of: " + score);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
    }

    /**
     * See runQuery(String fullQuery, int numHits)
     * Default's numHits to 20.
     * 
     * @param fullQuery
     * @return
     */
    public List<ResultClass> runQueries(String fullQuery) {
        return runQueries(fullQuery, 20);
    }


    /**
     * Checks to see if there is a wikipedia article with the same title
     * as the answer of a jeopardy question, for all jeopardy questions.
     * 
     * Must call getJQuestions(String path) before this method.
     */
    public void checkExistence() {

        // Loop over all jeopardy questions
        for (String[] jQ : jQuestions) {
            String[] query = jQ[ANS_INDEX].split(" ");

            String fullQuery = "";
            for (String s : query) {
                fullQuery += s + " OR ";
            }
            fullQuery = fullQuery.substring(0, fullQuery.length()-3);

            // System.out.println("You entered the query: " + fullQuery);

            System.out.println("Printing the answers to query " + jQ[ANS_INDEX]);
            runQueries(fullQuery, 5);
            System.out.println("\n\n");
        }
    }

    /**
     * Reads the questions.txt file from the directory and creates an ArrayList of
     * Arrays, where each Array, A, contains all 3 lines per jeopardy question.
     * A[0] = the category, use catIndex
     * A[1] = the question itself, use qIndex
     * A[2] = the answer to the question, use ansIndex
     * @param path
     */
    public void getJQuestions(String path) {
        jQuestions = new ArrayList<>();

        try (Scanner jQuestionsFile = new Scanner(new File(path + "/questions.txt"))) {
            while (jQuestionsFile.hasNextLine()) {
                String category = jQuestionsFile.nextLine();
                String question = jQuestionsFile.nextLine();
                String answer = jQuestionsFile.nextLine();
                jQuestionsFile.nextLine();  // Skips the empty line that follows

                String[] q = {category, question, answer};
                jQuestions.add(q);
            }
            jQuestionsFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("Jeopardy questions file not found. Critical error.\n");
        }
        System.out.println("Generated " + jQuestions.size()+ " jeopardy questions.\n");
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
