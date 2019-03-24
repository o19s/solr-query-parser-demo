package com.o19s.solr.qparser.capitalization;

import com.o19s.solr.qparser.IQueryParser;
import com.o19s.solr.qparser.analysis.AnalyzerUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.payloads.SpanPayloadCheckQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CapitalizationQParserSimple implements IQueryParser {

    private static final Logger LOG = LoggerFactory.getLogger(CapitalizationQParserSimple.class);
    private String fieldName;
    private Analyzer analyzer;

    public CapitalizationQParserSimple(String fieldName, Analyzer analyzer) {
        this.fieldName = fieldName;
        this.analyzer = analyzer;
    }

    @Override
    public Query parse(String qstr, int mmAsPercent) throws SyntaxError {

        String usage = "Usage: [firstcap|allcap|cap](term). Example: firstcap(trump)";
        String invalidSyntaxErrMsg = "Invalid syntax \"%s\"; %s";

        Pattern pattern = Pattern.compile("(?i)(firstcap|allcap|cap)\\((\\s*\\S+\\s*)\\)");
        Matcher matcher = pattern.matcher(qstr.trim());
        if (!matcher.find()) {
            throw new SyntaxError(String.format(invalidSyntaxErrMsg, qstr, usage));
        }

        String operator = matcher.group(1).trim().toLowerCase();
        String searchTerm = matcher.group(2).trim();

        LOG.info("Op=\"{}\", term=\"{}\"", operator, searchTerm);

        List<String> analyzedTerms = AnalyzerUtils.analyze(analyzer, fieldName, searchTerm);

        if (analyzedTerms.size() > 1) {
            throw new SyntaxError(String.format(invalidSyntaxErrMsg, qstr, usage));
        }

        return new SpanPayloadCheckQuery(
                new SpanTermQuery(new Term(fieldName, analyzedTerms.get(0))),
                Collections.singletonList(new BytesRef(operator)));

    }
}
