# Setup

* Clone project
* Run QueryEngine

# Additional information

* The FilesParser.java file was used to create the text documents in the wiki-subset-20140602-shortened folder. These are the wikipedia files with everything after the first paragraph deleted, which is what we used for our index. You do not need to run this file.
* The QueryEngine should run as is, but it requires both the files in the folder mentioned above and the questions.txt document with the 100 jeopardy questions in the specified format.
* To manually enter queries instead of automatically running all 100 sample Jeopardy questions comment lines 58 and 59 and uncomment line 62.
* If you want to see the results of all 100 questions look at output100.txt, and if you want to see the results for each manual query look at output.txt.
* See the py_instructions.txt folder for specific instructions related to running the transformer.
* Set the usetransformer boolean in QueryEngine to true to use the transformer.

