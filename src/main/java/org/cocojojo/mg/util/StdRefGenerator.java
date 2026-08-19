package org.cocojojo.mg.util;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.repository.StudentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StdRefGenerator {

  private final StudentRepository studentRepository;

  public String generate(int entryYear) {
    var prefix = "STD" + String.format("%02d", entryYear % 100);
    // Serialize generation for this entry year: the advisory lock guards the read-increment-insert
    // sequence (and the very first insert, which has no row to lock), while the FOR UPDATE read
    // makes any concurrent reader wait for the lock holder to commit. Must run inside the same
    // transaction as the student insert (see StudentService.create).
    studentRepository.lockStdPrefix(prefix);
    return studentRepository
        .findLastStdStartingWithForUpdate(prefix)
        .map(last -> prefix + String.format("%03d", nextSequence(last, prefix)))
        .orElse(prefix + "001");
  }

  private int nextSequence(String lastStd, String prefix) {
    return Integer.parseInt(lastStd.substring(prefix.length())) + 1;
  }
}
