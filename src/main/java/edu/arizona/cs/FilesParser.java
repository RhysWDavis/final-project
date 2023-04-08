package edu.arizona.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FilesParser {

    /**
     * Reads in an input file, following the format of Mihai's wikipedia collection,
     * and creates a new file containing only the title and first paragraph (or so)
     * of text per wikipedia article.
     * 
     * @param args - index 0 should be the path to the desired file to parse. 
     */
    public static void main(String[] args) {
        try {
            //String filename = args[0];
            String filename = "/Users/lilbig/Desktop/wiki_temp_subset/enwiki-20140602-pages-articles.xml-0005.txt";

            String newFileName = filename.substring(0, filename.length()-4) + "_last_paragraph.txt";
            File fileToWrite = new File(newFileName);
            FileWriter writerFile = new FileWriter(fileToWrite);
            parseFile(filename, writerFile);
            // generateArticleTitles(filename, writerFile);

            writerFile.close();
        } catch (IOException e) {
            System.out.println("Error while reading or writing to files.");
        }
    }

    /**
     * 
     * @param input - a scanner object to read in the lines of the input file
     * @param writerFile - the FileWriter object to write to
     * @throws IOException
     */
    private static void parseFile(String inputFileName, FileWriter writerFile) throws IOException {
        try (BufferedReader input = new BufferedReader(new FileReader(inputFileName))) { 
            String line = "";
            boolean isBreak = false;
            while ((line = input.readLine()) != null) {
                if ((line.startsWith("[[") && line.endsWith("]]")) || isBreak) {
                    boolean firstLine = true;
                    isBreak = false;
                    while (!line.startsWith("==") && !line.endsWith("==")) {
                        if (line.startsWith("[[") && line.endsWith("]]") && !firstLine) {
                            isBreak = true;
                            break;
                        }
                        firstLine = false;
                        writerFile.write(line + "\n");
                        line = input.readLine();
                    }
                    if (isBreak) {
                        writerFile.write("\nEnd of paragraph.[]\n\n" + line);
                    } else {
                        writerFile.write(line + "\nEnd of paragraph.[]\n\n");
                    }
                }
            }
        }
        writerFile.close();
    }

    public static void generateArticleTitles(String inputFileName, FileWriter writerFile) throws IOException {
        try (BufferedReader input = new BufferedReader(new FileReader(inputFileName))) { 
            String line = "";
            while ((line = input.readLine()) != null) { 
                if (line.startsWith("[[") && line.endsWith("]]")) {
                    writerFile.write(line + "\n");
                }
                // if (line.contains("[[")) {
                //     writerFile.write(line + "\n");
                // }
            }
        }
        writerFile.close();
    }

}
