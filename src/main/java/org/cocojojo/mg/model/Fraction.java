package org.cocojojo.mg.model;

public record Fraction(int numerator, int denominator) {

  public Fraction {
    if (denominator == 0) {
      throw new ArithmeticException("Denominator must not be zero");
    }
    int sign = denominator < 0 ? -1 : 1;
    int divisor = gcd(Math.abs(numerator), Math.abs(denominator));
    numerator = sign * numerator / divisor;
    denominator = sign * denominator / divisor;
  }

  private static int gcd(int a, int b) {
    return b == 0 ? a : gcd(b, a % b);
  }
}
