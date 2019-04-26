package com.o19s.solr.qparser;

import com.o19s.solr.qparser.proximity.ProximityQParserSimple;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.solr.search.SyntaxError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ProximityQParserSimpleTest {

    private static Logger logger = LoggerFactory.getLogger(MyQParserTest.class);
    private static LuceneIndex luceneIndex;
    private static ProximityQParserSimple proximityQParserSimple;

    @BeforeClass
    public static void setup() throws IOException {
        luceneIndex = LuceneIndex.getInstance();
        proximityQParserSimple = new ProximityQParserSimple("title", luceneIndex.getAnalyzer());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (luceneIndex != null)
            luceneIndex.tearDown();
    }

    private IndexSearcher getSearcher() {
        return luceneIndex.getSearcher();
    }

    private IndexReader getReader() {
        return luceneIndex.getReader();
    }

    @Test
    public void testWithTwoTerms() throws SyntaxError, IOException {
        String qstr = "Fox w4 Dog";
        Query actualQuery = proximityQParserSimple.parse(qstr, 100);

        String fieldName = "title";

        // Verify that the generated Lucene query is as expected:
        Query spanQuery = new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term(fieldName, "fox")),
                        new SpanTermQuery(new Term(fieldName, "dog")),
                },
                4,
                true);

        BooleanQuery.Builder builder = new BooleanQuery.Builder()
                .add(spanQuery, BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(1);

        Query expectedQuery = builder.build();
        logger.debug("Actual   query: {}", actualQuery);
        logger.debug("Expected query: {}", expectedQuery);

        assertEquals(expectedQuery, actualQuery);

        // Test the generated Lucene query against the test RAM-based Lucene index:
        TopDocs hits = getSearcher().search(actualQuery, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }

    @Test
    public void testWithFourTerms() throws SyntaxError, IOException {
        String qstr = "Brown Fox w4 Dog Lazy";
        Query actualQuery = proximityQParserSimple.parse(qstr, 100);

        String fieldName = "title";

        // Verify that the generated Lucene query is as expected:
        Query spanQuery = new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term(fieldName, "fox")),
                        new SpanTermQuery(new Term(fieldName, "dog")),
                },
                4,
                true);

        BooleanQuery.Builder builder = new BooleanQuery.Builder()
                .add(new TermQuery(new Term(fieldName, "brown")), BooleanClause.Occur.SHOULD)
                .add(spanQuery, BooleanClause.Occur.SHOULD)
                .add(new TermQuery(new Term(fieldName, "lazy")), BooleanClause.Occur.SHOULD)
                .setMinimumNumberShouldMatch(3);

        Query expectedQuery = builder.build();
        logger.debug("Actual   query: {}", actualQuery);
        logger.debug("Expected query: {}", expectedQuery);

        assertEquals(expectedQuery, actualQuery);

        // Test the generated Lucene query against the test RAM-based Lucene index:
        TopDocs hits = getSearcher().search(actualQuery, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }
}
