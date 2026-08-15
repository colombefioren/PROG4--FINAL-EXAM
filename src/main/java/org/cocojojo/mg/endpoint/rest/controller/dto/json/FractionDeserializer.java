package org.cocojojo.mg.endpoint.rest.controller.dto.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.cocojojo.mg.model.Fraction;

public class FractionDeserializer extends JsonDeserializer<Fraction> {

  @Override
  public Fraction deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);
    if (node.isObject()) {
      return new Fraction(node.get("numerator").asInt(), node.get("denominator").asInt());
    }
    if (node.isTextual()) {
      return parseText(node.asText());
    }
    if (node.isNumber()) {
      return fromDecimal(node.decimalValue());
    }
    throw new IOException(
        "Unsupported coefficient format, expected an object, a \"n/d\" string or a decimal: "
            + node);
  }

  private Fraction parseText(String text) {
    var trimmed = text.trim();
    if (trimmed.contains("/")) {
      var parts = trimmed.split("/", 2);
      return new Fraction(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }
    return fromDecimal(new BigDecimal(trimmed));
  }

  private Fraction fromDecimal(BigDecimal decimal) {
    var normalized = decimal.stripTrailingZeros();
    if (normalized.scale() < 0) {
      normalized = normalized.setScale(0);
    }
    var denominator = BigInteger.TEN.pow(normalized.scale());
    var numerator = normalized.unscaledValue();
    return new Fraction(numerator.intValueExact(), denominator.intValueExact());
  }
}
