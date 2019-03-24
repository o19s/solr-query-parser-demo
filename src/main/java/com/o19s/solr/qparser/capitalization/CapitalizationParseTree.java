package com.o19s.solr.qparser.capitalization;

import com.o19s.solr.analysis.CapitalizationPayloadEnum;

class CapitalizationParseTree {

    private CapitalizationPayloadEnum payloadEnum;
    private String searchTerm;

    CapitalizationParseTree(CapitalizationPayloadEnum payloadEnum, String searchTerm) {
        this.payloadEnum = payloadEnum;
        this.searchTerm = searchTerm;
    }

    CapitalizationPayloadEnum getPayloadEnum() {
        return payloadEnum;
    }

    String getSearchTerm() {
        return searchTerm;
    }
}
