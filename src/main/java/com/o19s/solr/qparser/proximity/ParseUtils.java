package com.o19s.solr.qparser.proximity;

import com.o19s.solr.qparser.analysis.AnalyzerUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProximityQParserSimple.class);

    public static ProximityParseTree parse(String qstr, String fieldName, Analyzer analyzer) throws SyntaxError {

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
        // term1 OR ... termN-1 OR (termN w5 termN+1) OR termN+2 ... OR termM

        // Analyze the search terms
        List<String> leftTerms = AnalyzerUtils.analyze(analyzer, fieldName, leftSide);
        int leftTermsCount = leftTerms.size();
        List<String> rightTerms = AnalyzerUtils.analyze(analyzer, fieldName, rightSide);
        int rightTermsCount = rightTerms.size();

        // Proximity's distance & order
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

        return new ProximityParseTree(
                proximityOperator,
                distance,
                inOrder,
                leftTerms,
                rightTerms
        );
    }
}
