package com.o19s.solr.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.IdentityEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;

import java.io.IOException;
import java.util.Objects;

public final class CapitalizationPayloadTokenFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
    private final PayloadEncoder encoder = new IdentityEncoder();

    CapitalizationPayloadTokenFilter(TokenStream input) {
        super(input);
    }

    private CharTermAttribute getTermAtt() {
        return termAtt;
    }

    private PayloadAttribute getPayAtt() {
        return payAtt;
    }

    private PayloadEncoder getEncoder() {
        return encoder;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }

        final char[] buffer = termAtt.buffer();
        final int length = termAtt.length();
        boolean someUpperCase = false;
        boolean someLowerCase = false;
        boolean firstCharIsUpperCase = false;
        int upperCaseCount = 0;
        for (int i = 0; i < length; i++) {
            if (Character.isUpperCase(buffer[i])) {
                someUpperCase = true;
                if (0 == i)
                    firstCharIsUpperCase = true;
                upperCaseCount++;
            } else {
                someLowerCase = true;
            }
        }

        if (someUpperCase) {
            CapitalizationPayloadEnum payloadEnum;
            if (!someLowerCase) {
                // All upper case
                payloadEnum = CapitalizationPayloadEnum.ALLCAP;
            } else if (firstCharIsUpperCase && 1 == upperCaseCount) {
                // First char is upper case only
                payloadEnum = CapitalizationPayloadEnum.FIRSTCAP;
            } else {
                // Some lower case
                payloadEnum = CapitalizationPayloadEnum.CAP;
            }

            if (payAtt.getPayload() != null) {
                throw new IllegalArgumentException(String.format("A payload is already set for token %s", new String(buffer)));
            }

            payAtt.setPayload(encoder.encode(payloadEnum.getPayload().toCharArray()));
        }

        return true;
    }

    /**
     * Overridden equals as required by Sonar.
     *
     * @param obj The object being compared to.
     * @return true if the passed object is equal to this object.
     */
    @Override
    public boolean equals(Object obj) {
        // The parents' parts must be equal
        if (!super.equals(obj)) {
            return false;
        }

        // The object's additional fields must be equal
        CapitalizationPayloadTokenFilter filter = (CapitalizationPayloadTokenFilter) obj;

        return filter.getTermAtt().equals(termAtt) &&
                filter.getPayAtt().equals(payAtt) &&
                filter.getEncoder().equals(encoder);
    }

    /**
     * Companion to equals.
     *
     * @return The object's unique hash value.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 89 * hash + Objects.hash(termAtt);
        hash = 89 * hash + Objects.hash(payAtt);
        hash = 89 * hash + Objects.hash(encoder);
        return hash;
    }
}
