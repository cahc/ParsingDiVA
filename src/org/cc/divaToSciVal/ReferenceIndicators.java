package org.cc.divaToSciVal;

/**
 * Observed and Swedish subject-reference values for one matched SciVal publication.
 */
public record ReferenceIndicators(
        int observedTop10,
        int observedTop50,
        int observedInternational,
        double expectedTop10,
        double expectedTop50,
        double expectedInternational,
        int referenceSetSize,
        int internationalReferenceSetSize,
        boolean usedCitationFallback,
        boolean usedInternationalFallback,
        String normalizationArtifactGroup,
        ReferenceScope citationReferenceScope,
        ReferenceScope internationalReferenceScope,
        int citationReferencePopulationSize,
        int internationalReferencePopulationSize) {
}
