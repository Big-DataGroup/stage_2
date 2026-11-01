package org.ulpgc.bigdata.indexing;

import java.util.Set;

public class IndexManager {
    private final InvertedIndex index;
    private final Tokenizer tokenizer;

    public IndexManager() {
        this.index = new InvertedIndex();
        this.tokenizer = new Tokenizer();
    }

    public void indexText(int bookId, String rawContent) {
        Set<String> tokens = tokenizer.tokenize(rawContent);
        index.addDocument(bookId, tokens);
    }

    public InvertedIndex getIndex() {
        return index;
    }
}