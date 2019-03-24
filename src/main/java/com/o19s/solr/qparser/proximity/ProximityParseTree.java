package com.o19s.solr.qparser.proximity;

import java.util.Collections;
import java.util.List;

public class ProximityParseTree {

    String operator;
    int distance;
    boolean inOrder;
    List<String> leftTerms;
    List<String> rightTerms;

    public ProximityParseTree(String operator, int distance, boolean inOrder, List<String> leftTerms, List<String> rightTerms) {
        this.operator = operator;
        this.distance = distance;
        this.inOrder = inOrder;
        this.leftTerms = (leftTerms == null || leftTerms.isEmpty()) ? Collections.emptyList() : leftTerms;
        this.rightTerms = (rightTerms == null || rightTerms.isEmpty()) ? Collections.emptyList() : rightTerms;
    }

    public String getOperator() {
        return operator;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isInOrder() {
        return inOrder;
    }

    public List<String> getLeftTerms() {
        return leftTerms;
    }

    public List<String> getRightTerms() {
        return rightTerms;
    }
}
