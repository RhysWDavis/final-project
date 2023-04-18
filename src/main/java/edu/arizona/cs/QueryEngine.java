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
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
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

@SuppressWarnings("unused")
public class QueryEngine {
    private boolean indexExists = false;
    private String inputDirPath = "";
    private Directory index;
    private Analyzer analyzer = new StandardAnalyzer();
    private ArrayList<String[]> jQuestions;
    private float reciprocalRankSum = 0;
    private int numCorrect = 0;
    private static final int CAT_INDEX = 0;
    private static final int Q_INDEX = 1;
    private static final int ANS_INDEX = 2;

    public static void main(String[] args) {
        try {
            String path = "/Users/sgrim/Desktop/483_final_project";
            QueryEngine objQueryEngine = new QueryEngine(path);
            objQueryEngine.getJQuestions(path);

            // objQueryEngine.runQs();
            objQueryEngine.checkJQuestions();
            // objQueryEngine.checkExistence();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public QueryEngine(String filesDir) {
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
        // Get file from resources folder
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
                    inputScanner.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            w.close();
        } catch (Exception e) {
        }
        indexExists = true;
    }

    public void runQs() throws java.io.FileNotFoundException, java.io.IOException {
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
            fullQuery = fullQuery.substring(0, fullQuery.length() - 3);

            // System.out.println("You entered the query: " + fullQuery);
            runQueries(fullQuery, q);

            System.out.println("Please enter a query (or STOP)\n");
            q = userInput.nextLine();
        }
        userInput.close();
    }

    /**
     * Performs the functionality of giving a query to the index, obtaining the
     * results,
     * and printing out and returning some of them.
     * 
     * @param fullQuery - The exact query to be passed to the index
     * @return a list of ResultClass objects of each document matching the fullQuery
     */
    public List<ResultClass> runQueries(String fullQuery, int numHits, String answer) {
        List<ResultClass> ans = new ArrayList<ResultClass>();

        try {
            Query q = new QueryParser("text", analyzer).parse(fullQuery);
            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            LMDirichletSimilarity similar = new LMDirichletSimilarity();
            searcher.setSimilarity(similar);
            if (numHits == 0) {
                return null;
            }
            TopDocs docs = searcher.search(q, numHits);
            ScoreDoc[] hits = docs.scoreDocs;

            // For each document matching the query, add it to the list and print out some
            // info
            for (int i = 0; i < hits.length; ++i) {
                int docID = hits[i].doc;
                Document d = searcher.doc(docID);

                ResultClass result = new ResultClass();
                result.DocName = d;
                result.docScore = hits[i].score;
                ans.add(result);
                double score = Math.round(result.docScore * 1000d) / 1000d;
                float rank = (float) 1 / (float) (i + 1);
                if (result.DocName.get("docName").equals(answer.trim())) {
                    if (i == 0) {
                        numCorrect++;
                    }
                    this.reciprocalRankSum += rank;
                    System.out.println("The document: " + result.DocName.get("docName") + " had a score of: " + score
                            + " the reciprocal rank is " + String.valueOf(rank));
                } else if (answer.contains("|")) {
                    String[] answers = answer.split("\\|", 2);
                    if (result.DocName.get("docName").equals(answers[0].trim())
                            || result.DocName.get("docName").equals(answers[1].trim())) {
                        System.out
                                .println("The document: " + result.DocName.get("docName") + " had a score of: " + score
                                        + " the reciprocal rank is " + String.valueOf(rank));
                        this.reciprocalRankSum += rank;
                        if (i == 0) {
                            numCorrect++;
                        }
                    } else {
                        System.out.println(
                                "The document: " + result.DocName.get("docName") + " had a score of: " + score);
                    }

                } else {
                    System.out.println("The document: " + result.DocName.get("docName") + " had a score of: " + score);
                }
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
    public List<ResultClass> runQueries(String fullQuery, String answer) {
        return runQueries(fullQuery, 20, answer);
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
            fullQuery = fullQuery.substring(0, fullQuery.length() - 3);

            // System.out.println("You entered the query: " + fullQuery);

            System.out.println("Printing the answers to query " + jQ[ANS_INDEX]);
            runQueries(fullQuery, jQ[ANS_INDEX]);
            System.out.println("\n\n");
        }
        System.out.println("Mean Reciprical Rank: " + String.valueOf(reciprocalRankSum / jQuestions.size()));
        System.out.println(String.valueOf(numCorrect) + " out of " + String.valueOf(jQuestions.size())
                + " questions were answered correctly");
    }

    public void checkJQuestions() {

        // Loop over all jeopardy questions
        for (String[] jQ : jQuestions) {
            String[] query = jQ[Q_INDEX].split(" ");

            String fullQuery = "";
            for (String s : query) {
                fullQuery += s + " OR ";
            }
            fullQuery = fullQuery.substring(0, fullQuery.length() - 3);

            // System.out.println("You entered the query: " + fullQuery);

            System.out.println("Printing the answers to query " + jQ[Q_INDEX]);
            runQueries(fullQuery, 1000, jQ[ANS_INDEX]);
            System.out.println("\n\n");
        }
        System.out.println("Mean Reciprical Rank: " + String.valueOf(reciprocalRankSum / jQuestions.size()));
        System.out.println(String.valueOf(numCorrect) + " out of " + String.valueOf(jQuestions.size())
                + " questions were answered correctly");
    }

    /**
     * Reads the questions.txt file from the directory and creates an ArrayList of
     * Arrays, where each Array, A, contains all 3 lines per jeopardy question.
     * A[0] = the category, use catIndex
     * A[1] = the question itself, use qIndex
     * A[2] = the answer to the question, use ansIndex
     * 
     * @param path
     */
    public void getJQuestions(String path) {
        jQuestions = new ArrayList<>();

        try (Scanner jQuestionsFile = new Scanner(new File(path + "/questions.txt"))) {
            while (jQuestionsFile.hasNextLine()) {
                String category = jQuestionsFile.nextLine();
                String question = jQuestionsFile.nextLine();
                String answer = jQuestionsFile.nextLine();
                jQuestionsFile.nextLine(); // Skips the empty line that follows

                String[] q = { category, question, answer };
                jQuestions.add(q);
            }
            jQuestionsFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("Jeopardy questions file not found. Critical error.\n");
        }
        System.out.println("Generated " + jQuestions.size() + " jeopardy questions.\n");
    }

}
