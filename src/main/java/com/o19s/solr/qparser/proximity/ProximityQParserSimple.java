package com.o19s.solr.qparser.proximity;

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

public class ProximityQParserSimple implements IQueryParser {

    private static final Logger LOG = LoggerFactory.getLogger(ProximityQParserSimple.class);
    private String fieldName;
    private Analyzer analyzer;

    public ProximityQParserSimple(String fieldName, Analyzer analyzer) {
        this.fieldName = fieldName;
        this.analyzer = analyzer;
    }

    @Override
    public Query parse(String qstr, int mmAsPercent) throws SyntaxError {

        // Parse the query
        ProximityParseTree parseTree = ParseUtils.parse(qstr, fieldName, analyzer);

        int leftTermsCount = parseTree.getLeftTerms().size();
        int rightTermsCount = parseTree.getRightTerms().size();

        // Compose the query

        // Terms before the proximity clause if any
        List<Query> leftSingleTermQueries = new ArrayList<>();
        for (int i = 0; i < leftTermsCount - 1; i++) {
            leftSingleTermQueries.add(
                    new TermQuery(new Term(fieldName, parseTree.getLeftTerms().get(i)))
            );
        }

        // Proximity clause
        Query proximityQuery = new SpanNearQuery(
                new SpanQuery[]{
                        new SpanTermQuery(new Term(fieldName, parseTree.getLeftTerms().get(leftTermsCount - 1))),
                        new SpanTermQuery(new Term(fieldName, parseTree.getRightTerms().get(0)))},
                parseTree.getDistance(),
                parseTree.isInOrder());

        // Terms after the proximity clause if any
        List<Query> rightSingleTermQueries = new ArrayList<>();
        for (int i = 1; i < rightTermsCount; i++) {
            rightSingleTermQueries.add(
                    new TermQuery(new Term(fieldName, parseTree.getRightTerms().get(i)))
            );
        }

        // OR the search clauses
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        int clausesCount = leftSingleTermQueries.size() + 1 + rightSingleTermQueries.size();
        Long clausesMinMatch = Math.round(clausesCount * ((double) mmAsPercent / 100.0));
        builder.setMinimumNumberShouldMatch(clausesMinMatch.intValue());
        LOG.info("Clauses count={}, mmAsPercent={} --> mm={}", clausesCount, mmAsPercent, clausesMinMatch);

        for (Query query : leftSingleTermQueries) {
            builder.add(query, BooleanClause.Occur.SHOULD);
        }

        builder.add(proximityQuery, BooleanClause.Occur.SHOULD);

        for (Query query : rightSingleTermQueries) {
            builder.add(query, BooleanClause.Occur.SHOULD);
        }

        return builder.build();
    }
}