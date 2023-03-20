package edu.arizona.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@SuppressWarnings("unused")
public class QueryEngine {
    boolean indexExists = false;
    String inputFilePath = "";
    Directory index;
    Analyzer analyzer;

    public QueryEngine(String inputFile){
        inputFilePath = inputFile;
        buildIndex();
    }

    private void buildIndex() {
        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(inputFilePath).getFile());

        // File file = new File(inputFilePath);
        
        try (Scanner inputScanner = new Scanner(file)) {
            Path path = Paths.get("src/main/java/edu/arizona/cs/");

            analyzer = new WhitespaceAnalyzer();
            index = FSDirectory.open(path);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            w.deleteAll();

            // Adds each line of the input file to the index as a new document
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                int firstSpace = line.indexOf(" ");
                Document doc = new Document();
                doc.add(new StringField("docid", line.substring(0, firstSpace), Field.Store.YES));
                doc.add(new TextField("text", line.substring(firstSpace), Field.Store.YES));
                w.addDocument(doc);
            }
            
            // System.out.println("w has this many documents: " + w.numDocs());

            w.close();
            inputScanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexExists = true;
    }

    public static void main(String[] args ) {
        try {
            String fileName = "/Users/lilbig/Desktop/School/Spring 2023/CSC483/hw3_java-RhysWDavis/target/classes/input.txt";
            System.out.println("********Welcome to  Homework 3!");
            String[] query13a = {"information", "retrieval"};
            QueryEngine objQueryEngine = new QueryEngine(fileName);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public List<ResultClass> runQueries(String fullQuery) {
        if(!indexExists) {
            buildIndex();
        }
        List<ResultClass> ans = new ArrayList<ResultClass>();


        // String fullQuery = query[0] + " " + query[1];
        fullQuery = fullQuery.substring(0, fullQuery.length() - 1);
        try {
            Query q = new QueryParser("text", analyzer).parse(fullQuery);

            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            int numHits = searcher.count(q);
            TopDocs docs = searcher.search(q, numHits);
            ScoreDoc[] hits = docs.scoreDocs;

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

    public List<ResultClass> runQ1_1(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1_1 -----");
        String fullQuery = query[0] + " " + query[1];

        return runQueries(fullQuery);
    }

    public List<ResultClass> runQ1_2_a(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1_2_a -----");
        String fullQuery = query[0] + " AND " + query[1];

        return runQueries(fullQuery);
    }

    public List<ResultClass> runQ1_2_b(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1_2_b -----");
        String fullQuery = query[0] + " NOT " + query[1];

        return runQueries(fullQuery);
    }

    public List<ResultClass> runQ1_2_c(String[] query) throws java.io.FileNotFoundException,java.io.IOException {
        System.out.println("\n\n ----- RUNNING Q1_2_c -----");
        String fullQuery = "\"" + query[0] + " " + query[1] + "\"~1";

        return runQueries(fullQuery);
    }

    public List<ResultClass> runQ1_3(String[] query) throws java.io.FileNotFoundException,java.io.IOException {

        if(!indexExists) {
            buildIndex();
        }
        StringBuilder result = new StringBuilder("");
        List<ResultClass>  ans=new ArrayList<ResultClass>();
        ans =returnDummyResults(2);
        return ans;
    }


    private  List<ResultClass> returnDummyResults(int maxNoOfDocs) {

        List<ResultClass> doc_score_list = new ArrayList<ResultClass>();
            for (int i = 0; i < maxNoOfDocs; ++i) {
                Document doc = new Document();
                doc.add(new TextField("title", "", Field.Store.YES));
                doc.add(new StringField("docid", "Doc"+Integer.toString(i+1), Field.Store.YES));
                ResultClass objResultClass= new ResultClass();
                objResultClass.DocName =doc;
                doc_score_list.add(objResultClass);
            }

        return doc_score_list;
    }

}
