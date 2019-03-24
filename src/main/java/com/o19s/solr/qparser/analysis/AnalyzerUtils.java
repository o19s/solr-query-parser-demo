package com.o19s.solr.qparser.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyzerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerUtils.class);


    public static List<String> analyze(Analyzer analyzer, String fieldName, String text) throws SyntaxError {
        List<String> searchTerms = new ArrayList<>();
        try (TokenStream tokenStream = analyzer.tokenStream(fieldName, text)) {
            tokenStream.reset();

            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                searchTerms.add(termAttribute.toString());
            }

            LOG.info("Analyzed search terms: {}", searchTerms);
        } catch (IOException ioe) {
            throw new SyntaxError("An error occurred during the analysis.", ioe);
        }

        return searchTerms;
    }
}
