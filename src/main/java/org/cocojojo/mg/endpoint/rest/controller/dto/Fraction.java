package org.cocojojo.mg.endpoint.rest.controller.dto;

public record Fraction(int numerator, int denominator) {
  public Fraction {
    if (denominator <= 0) {
      throw new IllegalArgumentException("denominator must be strictly positive");
    }
  }

  public static Fraction from(org.apache.commons.lang3.math.Fraction fraction) {
    return new Fraction(fraction.getNumerator(), fraction.getDenominator());
  }

  public org.apache.commons.lang3.math.Fraction toApacheFraction() {
    return org.apache.commons.lang3.math.Fraction.getFraction(numerator, denominator);
  }
}
