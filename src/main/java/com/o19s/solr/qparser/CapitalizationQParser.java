package com.o19s.solr.qparser;

import com.o19s.solr.qparser.capitalization.CapitalizationQParserSimple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapitalizationQParser extends QParser {
    private static final Logger LOG = LoggerFactory.getLogger(CapitalizationQParser.class);

    public CapitalizationQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
        super(qstr, localParams, params, req);
    }

    public Query parse() throws SyntaxError {
        // Get the field to query
        String qf = getParam("qf");
        LOG.info("qf: {}", qf);

        // Get the query-time analyzer for "qf"
        Analyzer analyzer = req.getCore().getLatestSchema().getFieldType(qf).getQueryAnalyzer();

        IQueryParser parser = new CapitalizationQParserSimple(qf, analyzer);

        Query query = parser.parse(qstr, 100);

        LOG.info("Query: {}", query);

        return query;
    }
}
