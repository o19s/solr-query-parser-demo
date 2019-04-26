package com.o19s.solr.qparser;

import com.o19s.solr.qparser.proximity.ProximityQParserSimple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProximityQParser extends QParser {
    private static final Logger LOG = LoggerFactory.getLogger(ProximityQParser.class);

    ProximityQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        super(qstr, localParams, params, req);
    }

    public Query parse() throws SyntaxError {
        // Get the field to query
        String qf = getParam("qf"); // Query field (single-field at the moment)
        String mm = getParam("mm"); // Minimum should match (as a percentage)
        LOG.info("qf: {}; mm: {}", qf, mm);

        // Get the query-time analyzer for "qf"
        Analyzer analyzer = req.getCore().getLatestSchema().getFieldType(qf).getQueryAnalyzer();

        // Use the "Simple" proximity query parser for illustration purposes
        IQueryParser parser = new ProximityQParserSimple(qf, analyzer);

        int mmAsPercent = 100;
        try {
            if (mm != null) {
                mmAsPercent = Integer.valueOf(mm);
            }
        } catch (NumberFormatException nfe) {
            throw new SyntaxError(String.format("Invalid mm format \"%s\"", mm), nfe);
        }

        Query query = parser.parse(qstr, mmAsPercent);

        LOG.info("Query: {}", query);

        return query;
    }
}
