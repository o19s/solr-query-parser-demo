package com.o19s.solr.qparser;

import com.o19s.solr.qparser.proximity.ProximityQParserWithPhrase;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.solr.search.SyntaxError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyQParserTest {

    private static Logger logger = LoggerFactory.getLogger(MyQParserTest.class);
    private static LuceneIndex luceneIndex;
    private static ProximityQParserWithPhrase myQParser;

    @BeforeClass
    public static void setup() throws IOException {
        luceneIndex = LuceneIndex.getInstance();
        myQParser = new ProximityQParserWithPhrase("title", luceneIndex.getAnalyzer());
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
    public void testRegex() {
        Pattern pattern = Pattern.compile("(.+)([w|n]\\d+)(.+)");
        Matcher matcher = pattern.matcher("hello w10 world");
        assertTrue(matcher.find());
        String leftOperand = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        String rightOperand = matcher.group(3).trim();

        System.out.println(String.format("left operand=\"%s\"", leftOperand));
        System.out.println(String.format("operator=\"%s\"", operator));
        System.out.println(String.format("right operand=\"%s\"", rightOperand));

        assertEquals("hello", leftOperand);
        assertEquals("w10", operator);
        assertEquals("world", rightOperand);

        pattern = Pattern.compile("(w|n)(\\d+)");
        matcher = pattern.matcher(operator);
        assertTrue(matcher.find());
        String proximityOperator = matcher.group(1).trim();
        int distance = Integer.parseInt(matcher.group(2));

        assertEquals("w", proximityOperator);
        assertEquals(10, distance);
    }

    @Test
    public void testDebugInfo() {
        String debugInfo = "\n0.27517414 = weight(spanOr([spanNear([title_txt:donald, title_txt:impeached], 10, true)]) in 0) [SchemaSimilarity], result of:\n  0.27517414 = score(doc=0,freq=0.33333334 = phraseFreq=0.33333334\n), product of:\n    0.5753642 = idf(), sum of:\n      0.2876821 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        1.0 = docFreq\n        1.0 = docCount\n      0.2876821 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        1.0 = docFreq\n        1.0 = docCount\n    0.47826084 = tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:\n      0.33333334 = phraseFreq=0.33333334\n      1.2 = parameter k1\n      0.75 = parameter b\n      5.0 = avgFieldLength\n      5.0 = fieldLength\n";
        System.out.println(debugInfo);
    }

    @Test
    public void testLuceneIndexIsRunning() throws IOException {
        Query query = new TermQuery(new Term("title", "fox"));
        logger.info("Query: {}", query);
        TopDocs hits = getSearcher().search(query, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }

    @Test
    public void testWithSingleTermOperands() throws SyntaxError, IOException {
        String qstr = "Fox w4 Dog";
        Query actualQuery = myQParser.parse(qstr, 100);

        Query expectedQuery = new SpanOrQuery(new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term("title", "fox")),
                        new SpanTermQuery(new Term("title", "dog")),
                },
                4,
                true));

        assertEquals(expectedQuery, actualQuery);

        TopDocs hits = getSearcher().search(actualQuery, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }

    @Test
    public void testWithMultiTermsOperands() throws SyntaxError, IOException {
        String qstr = "Quick fox w4 Lazy dog";
        Query actualQuery = myQParser.parse(qstr, 100);

        List<SpanQuery> spans = new ArrayList<>();
        spans.add(new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term("title", "quick")),
                        new SpanTermQuery(new Term("title", "lazy"))},
                4, true));
        spans.add(new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term("title", "quick")),
                        new SpanTermQuery(new Term("title", "dog"))},
                4, true));
        spans.add(new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term("title", "fox")),
                        new SpanTermQuery(new Term("title", "lazy"))},
                4, true));
        spans.add(new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term("title", "fox")),
                        new SpanTermQuery(new Term("title", "dog"))},
                4, true));

        Query expectedQuery = new SpanOrQuery(spans.toArray(spans.toArray(new SpanQuery[0])));

        assertEquals(expectedQuery, actualQuery);

        TopDocs hits = getSearcher().search(actualQuery, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }

    @Test
    public void testWithPhraseOperand() throws SyntaxError, IOException {
        String qstr = "Fox w4 \"Lazy dog\"";
        Query actualQuery = myQParser.parse(qstr, 100);

        List<SpanQuery> spans = new ArrayList<>();
        spans.add(new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term("title", "fox")),
                        new SpanNearQuery(new SpanQuery[]{
                                new SpanTermQuery(new Term("title", "lazy")),
                                new SpanTermQuery(new Term("title", "dog"))},
                                0, true)},
                4, true));

        Query expectedQuery = new SpanOrQuery(spans.toArray(spans.toArray(new SpanQuery[0])));

        assertEquals(expectedQuery, actualQuery);

        TopDocs hits = getSearcher().search(actualQuery, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }

    @Test
    public void testWithPhraseOperands() throws SyntaxError, IOException {
        String qstr = "\"Quick Brown Fox\" w4 \"Lazy Dog\"";
        Query actualQuery = myQParser.parse(qstr, 100);

        Query expectedQuery = new SpanNearQuery(
                new SpanQuery[]{
                        new SpanNearQuery(new SpanQuery[]{
                                new SpanTermQuery(new Term("title", "quick")),
                                new SpanTermQuery(new Term("title", "brown")),
                                new SpanTermQuery(new Term("title", "fox"))},
                                0, true),
                        new SpanNearQuery(new SpanQuery[]{
                                new SpanTermQuery(new Term("title", "lazy")),
                                new SpanTermQuery(new Term("title", "dog"))},
                                0, true)
                },
                4, true);

        assertEquals(expectedQuery, actualQuery);

        TopDocs hits = getSearcher().search(actualQuery, 10);
        assertEquals(1, hits.totalHits);
        assertEquals("0001", getReader().document(hits.scoreDocs[0].doc).get("id"));
    }
}
