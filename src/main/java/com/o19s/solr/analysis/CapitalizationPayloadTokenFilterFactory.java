package com.o19s.solr.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

public class CapitalizationPayloadTokenFilterFactory extends TokenFilterFactory {

    public CapitalizationPayloadTokenFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public CapitalizationPayloadTokenFilter create(TokenStream input) {
        return new CapitalizationPayloadTokenFilter(input);
    }
}
