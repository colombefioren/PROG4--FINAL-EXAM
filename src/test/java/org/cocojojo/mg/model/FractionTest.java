package org.cocojojo.mg.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FractionTest {

  @Test
  void zero_denominator_is_rejected() {
    assertThrows(IllegalArgumentException.class, () -> new Fraction(1, 0));
  }

  @Test
  void zero_numerator_is_rejected() {
    assertThrows(IllegalArgumentException.class, () -> new Fraction(0, 2));
  }

  @Test
  void numerator_above_denominator_is_rejected() {
    assertThrows(IllegalArgumentException.class, () -> new Fraction(3, 2));
  }

  @Test
  void negative_denominator_is_normalized() {
    var fraction = new Fraction(-2, -4);
    assertEquals(1, fraction.numerator());
    assertEquals(2, fraction.denominator());
  }

  @ParameterizedTest
  @CsvSource({"1,2,1,2", "2,4,1,2", "3,6,1,2", "4,4,1,1", "2,3,2,3", "100,200,1,2"})
  void fraction_is_reduced(
      int givenNumerator, int givenDenominator, int expectedNumerator, int expectedDenominator) {
    var fraction = new Fraction(givenNumerator, givenDenominator);
    assertEquals(expectedNumerator, fraction.numerator());
    assertEquals(expectedDenominator, fraction.denominator());
  }

  @Test
  void value_one_is_allowed() {
    var fraction = new Fraction(1, 1);
    assertEquals(1, fraction.numerator());
    assertEquals(1, fraction.denominator());
  }
}
