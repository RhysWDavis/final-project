package edu.arizona.cs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

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
            String filename = "/Users/lilbig/Desktop/wiki_temp_subset/enwiki-20140602-pages-articles.xml-0006.txt";

            Scanner input = new Scanner(new File(filename));
            String newFileName = filename.substring(0, filename.length()-4) + "_last_paragraph.txt";
            File fileToWrite = new File(newFileName);
            FileWriter writerFile = new FileWriter(fileToWrite);
            parseFile(input, writerFile);

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
    private static void parseFile(Scanner input, FileWriter writerFile) throws IOException {
        // String savedLine = "";
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.startsWith("[[") && line.endsWith("]]")) {
                while (!line.startsWith("==") && !line.endsWith("==")) {
                    if (line.startsWith("[[") && line.endsWith("]]")) {
                        // savedLine = line;
                        break;
                    }
                    writerFile.write(line + "\n");
                    line = input.nextLine();
                }
                writerFile.write(line + "\nEnd of paragraph.[]\n\n");
            }
        }
        System.out.println("hello");
    }
}
