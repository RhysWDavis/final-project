from sentence_transformers import SentenceTransformer, CrossEncoder, util
import os
import torch
from rank_bm25 import BM25Okapi
from sklearn.feature_extraction import _stop_words
import string
import numpy as np
import pandas as pd
import nltk
import warnings
import glob


nltk.download('punkt')

from nltk import sent_tokenize

warnings.filterwarnings("ignore")

auth_token = os.environ.get("auth_token")

bi_encoder_type = "multi-qa-mpnet-base-dot-v1"

top_k = 5
top_k*=2

path="/Users/karan/OneDrive/Desktop/college/483_final/wiki-subset-20140602-shortened"

path2="/Users/karan/OneDrive/Desktop/outputsamp.txt"

path3="/Users/karan/OneDrive/Desktop/inputsamp.txt"

necessary=[]
titles={}

bm25,corpus_embeddings=None,None
bi_encoder,  cross_encoder=None,None
docs=[]

def shorten():
    fl2=open(path2,'r',encoding='utf-8')
    txt=fl2.readlines()
    for line in txt:
        if line.startswith("The document"):
            wrds = line.split()
            end=wrds.index("had")
            necessary.append(" ".join(wrds[2:end]))
    fl2.close()

# Extract text from all shortened wikipedia pages created by
# FilesParser.java
# path is the path to the files
def extract_text_from_file():
    file_text=[]
    # Read all text files in directory
    my_files=glob.glob(path+"/*.txt")

    for fl in my_files:
        with open(fl,'r',encoding='utf-8') as f:
            file_text+=  f.readlines()
            f.close()
        
    return file_text
    
def preprocess_plain_text(text,window_size=3):
    global necessary
    #break into lines and remove leading and trailing space on each
    lines = [line.strip() for line in text]

    documents = []
    numDocs=0
    i=0
    doc_title=""
    doc_text=""
    while i<len(lines):
        if lines[i].startswith("[[")and lines[i][2:-2] in necessary:
            doc_title=lines[i][2:-2]
            doc_text=""
            i+=1
            while(i<len(lines)):
                if lines[i]=="End of paragraph.[]":
                    i+=1
                    numDocs+=1
                    break
                else:
                    doc_text+=lines[i]
                    i+=1
        else:
            i+=1
            continue
        documents.append(doc_text)
        titles[doc_text]=doc_title

    print(str(numDocs)+" Documents parsed")

    return documents


def bi_encode(bi_enc,passages):
    
    global bi_encoder, corpus_embeddings
    #We use the Bi-Encoder to encode all passages, so that we can use it with sematic search
    bi_encoder = SentenceTransformer(bi_enc,use_auth_token=auth_token)
    
    #quantize the model
    #bi_encoder = quantize_dynamic(model, {Linear, Embedding})

    #Compute the embeddings using the multi-process pool
    corpus_embeddings = bi_encoder.encode(passages, convert_to_tensor=True)

    print("Bi-encoder embeddings computed.")
    
    return bi_encoder, corpus_embeddings
    
def cross_encode():
    
    global cross_encoder
    #The bi-encoder will retrieve 100 documents. We use a cross-encoder, to re-rank the results list to improve the quality
    cross_encoder = CrossEncoder('cross-encoder/ms-marco-MiniLM-L-12-v2')
    return cross_encoder
    
def bm25_tokenizer(text):
    
# We also compare the results to lexical search (keyword search). Here, we use 
# the BM25 algorithm which is implemented in the rank_bm25 package.
# We lower case our text and remove stop-words from indexing
    tokenized_doc = []
    for token in text.lower().split():
        token = token.strip(string.punctuation)

        if len(token) > 0 and token not in _stop_words.ENGLISH_STOP_WORDS:
            tokenized_doc.append(token)
    return tokenized_doc

def bm25_api(passages):
    global bm25

    tokenized_corpus = []
    
    for passage in passages:
        tokenized_corpus.append(bm25_tokenizer(passage))

    bm25 = BM25Okapi(tokenized_corpus)
    
    return bm25

def display_df(model, top_k,score='score'):
    rnk=0
    for hit in model[0:top_k]:
        try:
            print(str(rnk+1)+":"+titles[docs[hit['corpus_id']]]+"-score " +str(hit[score]))
        except:
            print("something went wrong")
        rnk+=1

# This function will search all wikipedia articles for passages that
# answer the query
def search_func(query, bi_encoder_type, top_k=top_k):
    
    global bi_encoder, cross_encoder, bm25, corpus_embeddings
    

    ##### BM25 search (lexical search) #####
    bm25_scores = bm25.get_scores(bm25_tokenizer(query)) # type: ignore
    top_n = np.argpartition(bm25_scores, -5)[-5:]
    bm25_hits = [{'corpus_id': idx, 'score': bm25_scores[idx]} for idx in top_n]
    bm25_hits = sorted(bm25_hits, key=lambda x: x['score'], reverse=True)
    
    #print("Top-"+str(top_k)+" lexical search (BM25) hits")
    
   # bm25_df = display_df(bm25_hits,top_k)

    if bi_encoder_type == 'intfloat/e5-base':
        query = 'query: ' + query
    ##### Sematic Search #####
    # Encode the query using the bi-encoder and find potentially relevant passages
    question_embedding = bi_encoder.encode(query, convert_to_tensor=True) # type: ignore
    question_embedding = question_embedding.cpu() # type: ignore
    hits = util.semantic_search(question_embedding, corpus_embeddings, top_k=top_k,score_function=util.dot_score) # type: ignore
    hits = hits[0]  # Get the hits for the first query

    ##### Re-Ranking #####
    # Now, score all retrieved passages with the cross_encoder
    cross_inp = [[query, docs[hit['corpus_id']]] for hit in hits]
    cross_scores = cross_encoder.predict(cross_inp) # type: ignore
    #print("\nTop-"+str(top_k)+" bi-encoder hits")
    # Sort results by the cross-encoder scores
    for idx in range(len(cross_scores)):
        hits[idx]['cross-score'] = cross_scores[idx] # type: ignore

    # Output of top-k hits from bi-encoder
    hits = sorted(hits, key=lambda x: x['score'], reverse=True) # type: ignore
    #cross_df = display_df(hits,top_k)

    # Output of top-3 hits from re-ranker
    hits = sorted(hits, key=lambda x: x['cross-score'], reverse=True)
    print("Top-"+str(top_k)+" re-ranked cross encoder scores")
    rerank_df = display_df(hits,top_k,'cross-score')


import time

def main():
    global bm25
    global docs, bi_encoder, corpus_embeddings, cross_encoder
    t=time.localtime()
    current_time=time.strftime("%H:%M:%S",t)
    print(current_time)
    shorten()
    text = extract_text_from_file()

    docs=preprocess_plain_text(text)

    print("Loading "+bi_encoder_type+" bi-encoder and embedding document into vector space. This might take a few minutes")    

    bi_encoder, corpus_embeddings = bi_encode(bi_encoder_type,docs)
    cross_encoder = cross_encode()
    bm25 = bm25_api(docs)

    print("All Embeddings completed")
    t=time.localtime()
    current_time=time.strftime("%H:%M:%S",t)
    print(current_time)
    flag=False

    if(flag):
        f3=open(path3,'r',encoding='utf-8')
        ff=f3.readlines()
        for ln in ff:
            print(ln)
            search_func(ln,bi_encoder_type,top_k)
    else:
        while(True):
            search_query=input("Enter a Jeopardy question\n")
            search_func(search_query,bi_encoder_type,top_k)

if __name__=="__main__":
    main()