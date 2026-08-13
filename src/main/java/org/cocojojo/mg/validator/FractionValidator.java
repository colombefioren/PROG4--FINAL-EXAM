package org.cocojojo.mg.validator;

import java.util.function.Consumer;
import org.cocojojo.mg.model.Fraction;
import org.springframework.stereotype.Component;

@Component
public class FractionValidator implements Consumer<Fraction> {
  @Override
  public void accept(Fraction fraction) {
    if (fraction.numerator() <= 0 || fraction.numerator() > fraction.denominator()) {
      throw new IllegalArgumentException("coefficient must be in (0, 1]");
    }
  }
}
