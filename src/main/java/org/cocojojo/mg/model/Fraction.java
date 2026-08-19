package org.cocojojo.mg.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cocojojo.mg.endpoint.rest.controller.dto.json.FractionDeserializer;

@JsonDeserialize(using = FractionDeserializer.class)
public record Fraction(int numerator, int denominator) {

  public Fraction {
    if (denominator == 0) {
      throw new IllegalArgumentException("Denominator must not be zero");
    }
    int sign = denominator < 0 ? -1 : 1;
    int divisor = gcd(Math.abs(numerator), Math.abs(denominator));
    numerator = sign * numerator / divisor;
    denominator = sign * denominator / divisor;
    if (numerator <= 0 || numerator > denominator) {
      throw new IllegalArgumentException("Coefficient must be in (0, 1]");
    }
  }

  private static int gcd(int a, int b) {
    return b == 0 ? a : gcd(b, a % b);
  }
}
