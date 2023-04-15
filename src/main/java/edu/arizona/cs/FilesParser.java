package edu.arizona.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class FilesParser {

    public static void main(String[] args) {
        // String path = args[0];
        String path = "/Users/lilbig/Desktop/483_final_project";

        FilesParser fp = new FilesParser(path);
    }

    public FilesParser(String path) {
        // Gets the contents of the directory passed in
        File dir = new File(path);
        String[] dirContentTemp = dir.list();
        List<String> dirContent = Arrays.asList(dirContentTemp);

        // Checks to make sure the directory containing the wiki files exists
        if (!dirContent.contains("wiki-subset-20140602")) {
            System.out.println("The directory \"wiki-subset-20140602\" could not be found in"
                    + "the path given.\n Please enter a valid directory path.\n");
        }

        // Loop over all wiki files in the directory (there should be 80 of them)
        int i = 0;
        File longWikiDir = new File(path + "/wiki-subset-20140602");
        String[] longWikiDirContents = longWikiDir.list();
        for (String longWikiFileName : longWikiDirContents) {
            // Skip over any non .txt files (e.g. the Mac hidden file .DS_Store)
            if (!longWikiFileName.endsWith(".txt")) {
                continue;
            }

            // Create new directory for the shortened files to go
            String shortDirPath = path + "/wiki-subset-20140602-shortened";
            new File(shortDirPath).mkdirs(); // does not overwrite dir if it already exists

            // Get the longWikiFilePath and shortWikiFilePath
            String longWikiFilePath = longWikiDir.getAbsolutePath() + "/" + longWikiFileName;

            String shortWikiFileName = longWikiFileName.substring(0, longWikiFileName.length() - 4)
                    + "_last_paragraph.txt";
            String shortWikiFilePath = shortDirPath + "/" + shortWikiFileName;

            // Shorten the files, sending the shortened version to the newDir just created
            // System.out.println("From: " + longWikiFilePath + "\nTo: " + shortWikiFilePath
            // + "\n");
            createShortenedFile(longWikiFilePath, shortWikiFilePath);
            i++;
        }
        if (i == 80) {
            System.out.println("All 80 files shortened successfully (or they already existed).\n");
        } else {
            System.out.println("Something went wrong, only" + i + "files shortened, when 80 should have been.\n");
        }
    }

    /**
     * Reads in an input file, following the format of Mihai's wikipedia collection,
     * and creates a new file containing only the title and first paragraph (or so)
     * of text per wikipedia article.
     * 
     * @param args - index 0 should be the path to the desired file to parse.
     */
    public void createShortenedFile(String fileToShorten, String shortenedFileName) {
        try {
            File fileToWrite = new File(shortenedFileName);
            FileWriter writerFile = new FileWriter(fileToWrite);
            parseFile(fileToShorten, writerFile);
            // generateArticleTitles(filename, writerFile);

            writerFile.close();
        } catch (IOException e) {
            System.out.println("Error while reading or writing to files.");
        }
    }

    /**
     * 
     * @param input      - a scanner object to read in the lines of the input file
     * @param writerFile - the FileWriter object to write to
     * @throws IOException
     */
    private void parseFile(String inputFileName, FileWriter writerFile) throws IOException {
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

    public void generateArticleTitles(String inputFileName, FileWriter writerFile) throws IOException {
        try (BufferedReader input = new BufferedReader(new FileReader(inputFileName))) {
            String line = "";
            while ((line = input.readLine()) != null) {
                if (line.startsWith("[[") && line.endsWith("]]")) {
                    writerFile.write(line + "\n");
                }
                // if (line.contains("[[")) {
                // writerFile.write(line + "\n");
                // }
            }
        }
        writerFile.close();
    }

}
