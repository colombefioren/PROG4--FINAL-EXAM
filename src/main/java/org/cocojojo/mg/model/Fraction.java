package org.cocojojo.mg.model;

import jakarta.validation.constraints.Positive;

public record Fraction(int numerator, @Positive int denominator) {

  public static Fraction from(org.apache.commons.lang3.math.Fraction fraction) {
    return new Fraction(fraction.getNumerator(), fraction.getDenominator());
  }

  public org.apache.commons.lang3.math.Fraction toApacheFraction() {
    return org.apache.commons.lang3.math.Fraction.getFraction(numerator, denominator);
  }
}
