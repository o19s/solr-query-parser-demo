package com.o19s.solr.qparser.proximity;

import com.o19s.solr.analysis.AnalyzerUtils;
import com.o19s.solr.qparser.IQueryParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple proximity query parser that uses regular expressions to parse the end-user's search string.
 */
public class ProximityQParserSimple implements IQueryParser {

    private static final Logger LOG = LoggerFactory.getLogger(ProximityQParserSimple.class);
    private String fieldName;
    private Analyzer analyzer;

    public ProximityQParserSimple(String fieldName, Analyzer analyzer) {
        this.fieldName = fieldName;
        this.analyzer = analyzer;
    }

    /**
     * Implementation of the IQueryParser.parse() method.
     *
     * @param qstr        End-user's search string
     * @param mmAsPercent Minimum Should Match parameter (as a percentage)
     * @return The generated Lucene query
     * @throws SyntaxError A syntax error was encountered
     */
    @Override
    public Query parse(String qstr, int mmAsPercent) throws SyntaxError {

        // Query Parser flow steps 1 & 2: Parse and analyze the end-user's search string
        ProximityParseTree parseTree = buildParseTree(qstr);

        // Query Parser flow step 3: Compose the Lucene query
        // term1 .. termN w5 termN+1 .. termM
        // -->
        // (term1 OR ... termN-1) OR (termN w5 termN+1) OR (termN+2 ... OR termM)
        int leftTermsCount = parseTree.getLeftTerms().size();
        int rightTermsCount = parseTree.getRightTerms().size();

        // Terms before the proximity clause if any
        // term1  ... termN-1
        List<Query> leftSingleTermQueries = new ArrayList<>();
        for (int i = 0; i < leftTermsCount - 1; i++) {
            leftSingleTermQueries.add(
                    new TermQuery(new Term(fieldName, parseTree.getLeftTerms().get(i)))
            );
        }

        // Proximity clause
        // (termN w5 termN+1)
        Query proximityQuery = new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term(fieldName, parseTree.getLeftTerms().get(leftTermsCount - 1))),
                        new SpanTermQuery(new Term(fieldName, parseTree.getRightTerms().get(0)))},
                parseTree.getDistance(),
                parseTree.isInOrder());

        // Terms after the proximity clause if any
        // termN+2 ... termM
        List<Query> rightSingleTermQueries = new ArrayList<>();
        for (int i = 1; i < rightTermsCount; i++) {
            rightSingleTermQueries.add(
                    new TermQuery(new Term(fieldName, parseTree.getRightTerms().get(i)))
            );
        }

        // OR the search clauses
        // (term1 OR ... termN-1) OR (termN w5 termN+1) OR (termN+2 ... OR termM)

        // Use the BooleanQuery builder
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // Compute the number of clauses to match at a minimum
        int clausesCount = leftSingleTermQueries.size() + 1 + rightSingleTermQueries.size();
        Long clausesMinMatch = Math.round(clausesCount * ((double) mmAsPercent / 100.0));
        builder.setMinimumNumberShouldMatch(clausesMinMatch.intValue());
        LOG.info("Clauses count={}, mmAsPercent={} --> mm={}", clausesCount, mmAsPercent, clausesMinMatch);

        // OR the left-side clauses (if any)
        // term1 OR ... termN-1
        for (Query query : leftSingleTermQueries) {
            builder.add(query, BooleanClause.Occur.SHOULD);
        }

        // OR the proximity clause
        // termN w5 termN+1
        builder.add(proximityQuery, BooleanClause.Occur.SHOULD);

        // OR the right-side clauses (if any)
        // termN+2 ... OR termM
        for (Query query : rightSingleTermQueries) {
            builder.add(query, BooleanClause.Occur.SHOULD);
        }

        // Finally, build and return the generated query
        return builder.build();
    }

    /**
     * Parse the search string using regular expressions.
     *
     * @param qstr The end-user's search string
     * @return A parse tree
     * @throws SyntaxError A syntax error was encountered
     */
    private ProximityParseTree buildParseTree(String qstr) throws SyntaxError {

        // Parse with a regular expression: (left side)(prox. op)(right side)
        Pattern pattern = Pattern.compile("(.+)([w|n]\\d+)(.+)");
        Matcher matcher = pattern.matcher(qstr.trim());
        if (!matcher.find()) {
            throw new SyntaxError(
                    "Usage: one or more terms [w|n]<number> one or more terms. Example: hello w10 world");
        }
        String leftSide = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        String rightSide = matcher.group(3).trim();

        LOG.info("Left: \"{}\"; op=\"{}\"; right: \"{}\"", leftSide, operator, rightSide);

        // Compose the query
        // term1 .. termN w5 termN+1 .. termM
        // -->
        // (term1 OR ... termN-1) OR (termN w5 termN+1) OR (termN+2 ... OR termM)

        // Analyze the search terms
        List<String> leftTerms = AnalyzerUtils.analyze(analyzer, fieldName, leftSide);
        List<String> rightTerms = AnalyzerUtils.analyze(analyzer, fieldName, rightSide);

        // Parse the proximity's distance & order
        boolean inOrder = false;
        pattern = Pattern.compile("(w|n)(\\d+)");
        matcher = pattern.matcher(operator);
        if (!matcher.find()) {
            throw new SyntaxError(
                    String.format("Invalid operator syntax: \"%s\". Usage: w|n<number>. Example: hello w10 world",
                            operator));
        }
        String proximityOperator = matcher.group(1).trim().toLowerCase();
        if (proximityOperator.equalsIgnoreCase("w")) {
            // "w" specifies an ordered span
            inOrder = true;
        }
        int distance = Integer.parseInt(matcher.group(2));

        // Return a "parse tree" (quasi-AST)
        return new ProximityParseTree(
                proximityOperator,
                distance,
                inOrder,
                leftTerms,
                rightTerms
        );
    }
}