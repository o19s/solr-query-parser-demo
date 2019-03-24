package com.o19s.solr.qparser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LuceneIndex {

    private static Logger logger = LoggerFactory.getLogger(LuceneIndex.class);
    private static LuceneIndex luceneIndex;
    private Directory directory;
    private Analyzer analyzer;
    private IndexSearcher searcher;
    private IndexReader reader;

    private LuceneIndex() throws IOException {
        directory = new RAMDirectory();

        // Same as "general_text" in Solr schema.xml
        analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .build();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(directory, config)) {

            // Analyzed field type
            FieldType fieldTypeText = new FieldType(TextField.TYPE_STORED);
            fieldTypeText.setTokenized(true);
            fieldTypeText.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

            // Non-analyzed field type
            FieldType fieldTypeTextNonAnalyzed = new FieldType(TextField.TYPE_STORED);
            fieldTypeTextNonAnalyzed.setTokenized(false);

            Document doc = new Document();
            doc.add(new Field("id", "0001", fieldTypeTextNonAnalyzed));
            doc.add(new Field("title", "The Quick Brown Fox Jumps Over The Lazy Dog", fieldTypeText));

            writer.addDocument(doc);
        }

        // Create a searcher for the tests
        searcher = new IndexSearcher(DirectoryReader.open(directory));

        // Create a reader for the tests
        reader = searcher.getIndexReader();

        logger.info("RAM-based Lucene index created.");
    }

    static LuceneIndex getInstance() throws IOException {
        if (null == luceneIndex) {
            luceneIndex = new LuceneIndex();
        }
        return luceneIndex;
    }

    void tearDown() throws IOException {
        if (null != reader) {

            reader.close();
            reader = null;

            analyzer.close();
            analyzer = null;

            directory.close();
            directory = null;

            luceneIndex = null;

            logger.info("RAM-based Lucene index closed.");
        }
    }

    IndexSearcher getSearcher() {
        return searcher;
    }

    IndexReader getReader() {
        return reader;
    }

    Analyzer getAnalyzer() {
        return analyzer;
    }
}
