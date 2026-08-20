package org.cocojojo.mg.endpoint.rest.controller.dto.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.cocojojo.mg.model.Fraction;
import org.junit.jupiter.api.Test;

class FractionDeserializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializes_object_shape() throws IOException {
    var fraction = objectMapper.readValue("{\"numerator\":3,\"denominator\":4}", Fraction.class);

    assertEquals(new Fraction(3, 4), fraction);
  }

  @Test
  void deserializes_text_slash_shape() throws IOException {
    var fraction = objectMapper.readValue("\"2/4\"", Fraction.class);

    assertEquals(new Fraction(1, 2), fraction);
  }

  @Test
  void deserializes_text_decimal_shape() throws IOException {
    var fraction = objectMapper.readValue("\"0.5\"", Fraction.class);

    assertEquals(new Fraction(1, 2), fraction);
  }

  @Test
  void deserializes_numeric_shape() throws IOException {
    var fraction = objectMapper.readValue("0.25", Fraction.class);

    assertEquals(new Fraction(1, 4), fraction);
  }

  @Test
  void deserializes_repeating_decimal_with_bounded_precision() throws IOException {
    var fraction = objectMapper.readValue("0.333333", Fraction.class);

    assertEquals(new Fraction(333333, 1000000), fraction);
  }

  @Test
  void rejects_zero_denominator_text() {
    assertThrows(
        IllegalArgumentException.class, () -> objectMapper.readValue("\"1/0\"", Fraction.class));
  }

  @Test
  void rejects_unsupported_shape() {
    assertThrows(IOException.class, () -> objectMapper.readValue("[1,2]", Fraction.class));
  }
}
