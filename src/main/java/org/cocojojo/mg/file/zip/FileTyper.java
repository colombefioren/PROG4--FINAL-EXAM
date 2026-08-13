package org.cocojojo.mg.file.zip;

import static org.springframework.http.MediaType.parseMediaType;

import java.io.File;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.apache.tika.Tika;
import org.cocojojo.mg.PojaGenerated;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@PojaGenerated
@Component
public class FileTyper implements Function<File, MediaType> {

  @SneakyThrows
  @Override
  public MediaType apply(File file) {
    var tika = new Tika();
    String detectedMediaType = tika.detect(file);
    return parseMediaType(detectedMediaType);
  }
}
