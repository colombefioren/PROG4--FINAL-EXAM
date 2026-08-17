package org.cocojojo.mg.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.file.bucket.BucketComponent;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduateListService {

  private static final String BUCKET_KEY_PREFIX = "graduates/";
  private static final Duration PRESIGN_EXPIRATION = Duration.ofHours(1);

  private final PromotionRepository promotionRepository;
  private final StudentRepository studentRepository;
  private final ResultService resultService;
  private final BucketComponent bucketComponent;

  /**
   * Graduates of a promotion, ranked by descending overall average. A student graduates once every
   * course of their curriculum (L1 common + L2/L3 of their track) averages 10 or more.
   */
  public List<GraduateResponse> getGraduates(UUID promotionId) {
    assertPromotionExists(promotionId);
    var students = studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId);

    var summaries = resultService.computeResultsSummaries(students);
    var tracks = resultService.currentTracks(students);

    var graduates =
        students.stream()
            .map(s -> new GraduateCandidate(s, summaries.get(s.getId()), tracks.get(s.getId())))
            .filter(c -> Boolean.TRUE.equals(c.summary().graduate()))
            .sorted(
                Comparator.comparing(
                    (GraduateCandidate c) -> c.summary().overallAverage(),
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    var rows = new ArrayList<GraduateResponse>();
    int rank = 1;
    for (var candidate : graduates) {
      rows.add(
          GraduateResponse.builder()
              .rank(rank++)
              .std(candidate.student().getStd())
              .firstname(candidate.student().getFirstname())
              .lastname(candidate.student().getLastname())
              .track(candidate.track())
              .generalAverage(candidate.summary().overallAverage())
              .build());
    }
    return rows;
  }

  /** XLSX bytes of the graduate list, ready for direct download. */
  @SneakyThrows
  public byte[] buildXlsx(UUID promotionId) {
    var rows = getGraduates(promotionId);

    try (var workbook = new XSSFWorkbook()) {
      var sheet = workbook.createSheet("Diplomes");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("Rang");
      header.createCell(1).setCellValue("STD");
      header.createCell(2).setCellValue("Nom");
      header.createCell(3).setCellValue("Prenom");
      header.createCell(4).setCellValue("Moyenne generale");

      int rowIndex = 1;
      for (var row : rows) {
        var xlsxRow = sheet.createRow(rowIndex++);
        xlsxRow.createCell(0).setCellValue(row.rank());
        xlsxRow.createCell(1).setCellValue(row.std());
        xlsxRow.createCell(2).setCellValue(row.lastname());
        xlsxRow.createCell(3).setCellValue(row.firstname());
        xlsxRow
            .createCell(4)
            .setCellValue(row.generalAverage() == null ? 0 : row.generalAverage().doubleValue());
      }
      for (int i = 0; i < 5; i++) {
        sheet.autoSizeColumn(i);
      }

      var out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    }
  }

  /** Uploads the graduate list to S3 and returns a pre-signed download URL. */
  public String export(UUID promotionId) {
    var bytes = buildXlsx(promotionId);
    var bucketKey = BUCKET_KEY_PREFIX + buildFileName(promotionId);
    var tempFile = writeTempFile(promotionId, bytes);
    bucketComponent.upload(tempFile, bucketKey);
    return bucketComponent.presign(bucketKey, PRESIGN_EXPIRATION).toString();
  }

  /** Human-readable file name, e.g. PROMO-2023 - GRADUATE LIST - 1710000000000.xlsx. */
  public String buildFileName(UUID promotionId) {
    var promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Promotion with id:" + promotionId + " not found."));
    return promotion.getRef() + " - GRADUATE LIST - " + System.currentTimeMillis() + ".xlsx";
  }

  private void assertPromotionExists(UUID promotionId) {
    if (!promotionRepository.existsById(promotionId)) {
      throw new ResourceNotFoundException("Promotion with id:" + promotionId + " not found.");
    }
  }

  @SneakyThrows
  private File writeTempFile(UUID promotionId, byte[] bytes) {
    var tempFile = File.createTempFile("graduates-" + promotionId, ".xlsx");
    try (var fos = new FileOutputStream(tempFile)) {
      fos.write(bytes);
    }
    return tempFile;
  }

  private record GraduateCandidate(JStudent student, ResultsSummaryResponse summary, Track track) {}
}
