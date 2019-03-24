package com.o19s.solr.analysis;

import java.util.Arrays;
import java.util.Optional;

public enum CapitalizationPayloadEnum {

    FIRSTCAP("F", "firstcap"),
    ALLCAP("A", "allcap"),
    CAP("C", "cap");

    private String payload;
    private String operator;

    CapitalizationPayloadEnum(String payload, String operator) {
        this.payload = payload.toUpperCase();
        this.operator = operator.toLowerCase();
    }

    public static Optional<CapitalizationPayloadEnum> getPayloadForOperator(String operatorName) {
        return Arrays.stream(CapitalizationPayloadEnum.values())
                .filter(capEnum -> capEnum.getOperator().equalsIgnoreCase(operatorName))
                .findFirst();
    }

    public String getPayload() {
        return payload;
    }

    public String getOperator() {
        return operator;
    }
}
