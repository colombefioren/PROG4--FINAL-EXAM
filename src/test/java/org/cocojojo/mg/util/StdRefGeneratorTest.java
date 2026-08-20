package org.cocojojo.mg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.Optional;
import org.cocojojo.mg.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StdRefGeneratorTest {

  @Mock private StudentRepository studentRepository;

  private StdRefGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new StdRefGenerator(studentRepository);
  }

  @Test
  void generate_returns_001_when_no_previous_student() {
    given(studentRepository.findLastStdStartingWithForUpdate("STD24")).willReturn(Optional.empty());

    assertEquals("STD24001", generator.generate(2024));
    then(studentRepository).should().lockStdPrefix("STD24");
  }

  @Test
  void generate_increments_the_last_sequence() {
    given(studentRepository.findLastStdStartingWithForUpdate("STD24"))
        .willReturn(Optional.of("STD24042"));

    assertEquals("STD24043", generator.generate(2024));
  }

  @Test
  void generate_uses_two_digit_year_suffix() {
    given(studentRepository.findLastStdStartingWithForUpdate("STD04")).willReturn(Optional.empty());

    assertEquals("STD04001", generator.generate(2004));
  }

  @Test
  void generate_locks_the_prefix_before_looking_up() {
    given(studentRepository.findLastStdStartingWithForUpdate("STD24")).willReturn(Optional.empty());

    generator.generate(2024);

    var inOrder = inOrder(studentRepository);
    inOrder.verify(studentRepository).lockStdPrefix("STD24");
    inOrder.verify(studentRepository).findLastStdStartingWithForUpdate("STD24");
  }
}
