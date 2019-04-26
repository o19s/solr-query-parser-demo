package com.o19s.solr.qparser.proximity;

import java.util.Collections;
import java.util.List;

public class ProximityParseTree {

    private String operator;
    private int distance;
    private boolean inOrder;
    private List<String> leftTerms;
    private List<String> rightTerms;

    ProximityParseTree(String operator, int distance, boolean inOrder, List<String> leftTerms, List<String> rightTerms) {
        this.operator = operator;
        this.distance = distance;
        this.inOrder = inOrder;
        this.leftTerms = (leftTerms == null || leftTerms.isEmpty()) ? Collections.emptyList() : leftTerms;
        this.rightTerms = (rightTerms == null || rightTerms.isEmpty()) ? Collections.emptyList() : rightTerms;
    }

    public String getOperator() {
        return operator;
    }

    int getDistance() {
        return distance;
    }

    boolean isInOrder() {
        return inOrder;
    }

    List<String> getLeftTerms() {
        return leftTerms;
    }

    List<String> getRightTerms() {
        return rightTerms;
    }
}
