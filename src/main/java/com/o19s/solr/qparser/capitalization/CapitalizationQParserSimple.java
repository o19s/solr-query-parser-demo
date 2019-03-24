package com.o19s.solr.qparser.capitalization;

import com.o19s.solr.analysis.AnalyzerUtils;
import com.o19s.solr.analysis.CapitalizationPayloadEnum;
import com.o19s.solr.qparser.IQueryParser;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CapitalizationQParserSimple implements IQueryParser {

    private static final Logger LOG = LoggerFactory.getLogger(CapitalizationQParserSimple.class);
    private String fieldName;
    private Analyzer analyzer;

    /**
     * Constructor.
     *
     * @param fieldName Field name.
     * @param analyzer  Query-time analyzer.
     */
    public CapitalizationQParserSimple(String fieldName, Analyzer analyzer) {
        this.fieldName = fieldName;
        this.analyzer = analyzer;
    }

    /**
     * IQueryParser::parse implementation.
     *
     * @param qstr        Search string (As typed by the end-user).
     * @param mmAsPercent Minimum should match percentage (Not used here).
     * @return A Lucene query.
     * @throws SyntaxError When the search string does not adhere to the capitalization clause syntax.
     */
    @Override
    public Query parse(String qstr, int mmAsPercent) throws SyntaxError {

        CapitalizationParseTree parseTree = buildParseTree(qstr);

        return new SpanPayloadCheckQuery(
                new SpanTermQuery(new Term(fieldName, parseTree.getSearchTerm())),
                Collections.singletonList(new BytesRef(parseTree.getPayloadEnum().getPayload())));
    }

    /**
     * Parses a capitalization search clause, e.g., firstcap(dog), and produces a
     * rudimentary parse tree.
     *
     * @param qstr Search string.
     * @return A capitalization parse tree.
     */
    private CapitalizationParseTree buildParseTree(String qstr) throws SyntaxError {
        String usage = "Usage: [firstcap|allcap|cap](term). Example: firstcap(trump)";
        String invalidSyntaxErrMsg = "Invalid syntax \"%s\"; %s";

        Pattern pattern = Pattern.compile("(?i)(firstcap|allcap|cap)\\((\\s*\\S+\\s*)\\)");
        Matcher matcher = pattern.matcher(qstr.trim());
        if (!matcher.find()) {
            throw new SyntaxError(String.format(invalidSyntaxErrMsg, qstr, usage));
        }

        String operatorName = matcher.group(1).trim().toLowerCase();
        Optional<CapitalizationPayloadEnum> capEnumOpt = CapitalizationPayloadEnum.getPayloadForOperator(operatorName);
        if (!capEnumOpt.isPresent()) {
            throw new SyntaxError(String.format("Can't find the payload associated with the operator \"%s\"", operatorName));
        }
        String searchTerm = matcher.group(2).trim();

        LOG.info("Op=\"{}\", term=\"{}\"", operatorName, searchTerm);

        List<String> analyzedTerms = AnalyzerUtils.analyze(analyzer, fieldName, searchTerm);

        if (analyzedTerms.size() > 1) {
            throw new SyntaxError(String.format(invalidSyntaxErrMsg, qstr, usage));
        }

        return new CapitalizationParseTree(capEnumOpt.get(), analyzedTerms.get(0));
    }
}
