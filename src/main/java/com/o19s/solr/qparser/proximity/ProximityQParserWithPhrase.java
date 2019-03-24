package com.o19s.solr.qparser.proximity;

import com.o19s.solr.qparser.IQueryParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProximityQParserWithPhrase implements IQueryParser {

    private static final Logger LOG = LoggerFactory.getLogger(ProximityQParserWithPhrase.class);
    private String fieldName;
    private Analyzer analyzer;

    public ProximityQParserWithPhrase(String fieldName, Analyzer analyzer) {
        this.analyzer = analyzer;
        this.fieldName = fieldName;
    }

    @Override
    public Query parse(String qstr, int mmAsPercent) throws SyntaxError {
        Query query;
        Pattern pattern = Pattern.compile("(.+)([w|n]\\d+)(.+)");
        Matcher matcher = pattern.matcher(qstr.trim());
        if (!matcher.find()) {
            throw new SyntaxError("Usage: one or more terms [w|n]<number> one or more terms. Example: hello w10 world");
        }
        String leftOperand = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        String rightOperand = matcher.group(3).trim();

        // Are the operands phrases?
        pattern = Pattern.compile("\"(.*)\"");
        matcher = pattern.matcher(leftOperand);
        boolean leftOperandPhrase = false;
        if (matcher.find()) {
            leftOperandPhrase = true;
        }

        matcher = pattern.matcher(rightOperand);
        boolean rightOperandPhrase = false;
        if (matcher.find()) {
            rightOperandPhrase = true;
        }

        LOG.info("Left operand='{}' (phrase={}), operator={},right operand='{}' (phrase={})",
                leftOperand, leftOperandPhrase, operator, rightOperand, rightOperandPhrase);

        // Analyze the operands
        List<String> leftSearchTerms;
        List<String> rightSearchTerms;

        leftSearchTerms = getSearchTerms(analyzer, fieldName, leftOperand);
        rightSearchTerms = getSearchTerms(analyzer, fieldName, rightOperand);

        // span's distance & order
        boolean inOrder = false;
        pattern = Pattern.compile("(w|n)(\\d+)");
        matcher = pattern.matcher(operator);
        if (!matcher.find()) {
            throw new SyntaxError(
                    String.format("Invalid operator syntax: \"%s\". Usage: w|n<number>. Example: hello w10 world",
                            operator));
        }
        String proximityOperator = matcher.group(1).trim().toLowerCase();
        if (proximityOperator.equals("w")) {
            // "w" specifies an ordered span
            inOrder = true;
        }
        int distance = Integer.parseInt(matcher.group(2));

        if (leftOperandPhrase) {
            SpanQuery leftSpanQuery = getPhraseSpanQuery(fieldName, leftSearchTerms);

            if (rightOperandPhrase) {
                // Build the various span queries
                // Example: For "cat kitty" w10 "dog doggy"
                // Single span.
                SpanQuery rightSpanQuery = getPhraseSpanQuery(fieldName, rightSearchTerms);

                query = new SpanNearQuery(
                        new SpanQuery[]{
                                leftSpanQuery,
                                rightSpanQuery},
                        distance,
                        inOrder);
            } else {
                // Build the various span queries
                // Example: For "cat kitty" w10 dog doggy
                // spans:
                // "cat kitty" w10 dog
                // "cat kitty" w10 doggy
                List<SpanQuery> spans = new ArrayList<>();
                for (String rightSearchTerm : rightSearchTerms)
                    spans.add(new SpanNearQuery(
                            new SpanQuery[]{
                                    leftSpanQuery,
                                    new SpanTermQuery(new Term(fieldName, rightSearchTerm))},
                            distance,
                            inOrder));

                query = new SpanOrQuery(spans.toArray(new SpanQuery[0]));
            }
        } else if (rightOperandPhrase) {
            // Build the various span queries
            // Example: For cat kitty w10 "dog doggy"
            // spans:
            // cat w10 "dog doggy"
            // kitty w10 "dog doggy"
            SpanQuery rightSpanQuery = getPhraseSpanQuery(fieldName, rightSearchTerms);

            List<SpanQuery> spans = new ArrayList<>();
            for (String leftSearchTerm : leftSearchTerms)
                spans.add(new SpanNearQuery(
                        new SpanQuery[]{
                                new SpanTermQuery(new Term(fieldName, leftSearchTerm)),
                                rightSpanQuery},
                        distance,
                        inOrder));

            query = new SpanOrQuery(spans.toArray(new SpanQuery[0]));
        } else {
            // Build the various span queries
            // Example: For cat kitty w10 dog doggy
            // spans:
            // cat w10 dog
            // cat w10 doggy
            // kitty w10 dog
            // kitty w10 doggy
            List<SpanQuery> spans = new ArrayList<>();
            for (String leftTerm : leftSearchTerms)
                for (String rightTerm : rightSearchTerms)
                    spans.add(new SpanNearQuery(
                            new SpanQuery[]{
                                    new SpanTermQuery(new Term(fieldName, leftTerm)),
                                    new SpanTermQuery(new Term(fieldName, rightTerm))
                            },
                            distance,
                            inOrder));

            query = new SpanOrQuery(spans.toArray(new SpanQuery[0]));
        }

        return query;

    }

    private List<String> getSearchTerms(Analyzer analyzer, String fieldName, String text) throws SyntaxError {
        List<String> searchTerms = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream(fieldName, text)) {
            tokenStream.reset();

            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                searchTerms.add(termAttribute.toString());
            }

            LOG.info("Search terms: {}", searchTerms);
        } catch (IOException ioe) {
            throw new SyntaxError("An error occurred during the analysis.", ioe);
        }

        return searchTerms;
    }

    private SpanQuery getPhraseSpanQuery(String fieldName, List<String> searchTerms) {
        List<SpanTermQuery> spanTermQueries = new ArrayList<>();
        for (String leftTerm : searchTerms)
            spanTermQueries.add(new SpanTermQuery(new Term(fieldName, leftTerm)));
        return new SpanNearQuery(spanTermQueries.toArray(new SpanQuery[0]), 0, true);
    }
}
