package com.nutrihealth.diagnostic.domain.model;

/**
 * Canonical (FHIR-aligned) representation of a single lab observation.
 * loincCode corresponds to a FHIR Observation.code.coding[].code.
 */
public class BiomarkerObservation {

    private final String loincCode;
    private final String display;
    private final double value;
    private final String unit;
    private final InterpretationCode interpretation;

    public BiomarkerObservation(String loincCode, String display, double value,
                                 String unit, InterpretationCode interpretation) {
        this.loincCode = loincCode;
        this.display = display;
        this.value = value;
        this.unit = unit;
        this.interpretation = interpretation;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public String getDisplay() {
        return display;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public InterpretationCode getInterpretation() {
        return interpretation;
    }
}
