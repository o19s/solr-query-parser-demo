package com.o19s.solr.qparser;

import org.apache.lucene.search.Query;
import org.apache.solr.search.SyntaxError;

public interface IQueryParser {

    Query parse(String qstr, int mmAsPercent) throws SyntaxError;
}
