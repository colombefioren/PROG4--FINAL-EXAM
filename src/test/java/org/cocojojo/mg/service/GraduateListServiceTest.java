package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.file.bucket.BucketComponent;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraduateListServiceTest {

  @Mock private PromotionRepository promotionRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private ResultService resultService;
  @Mock private BucketComponent bucketComponent;

  @InjectMocks private GraduateListService service;

  private UUID promotionId;
  private JPromotion promotion;
  private JStudent elStudent;
  private JStudent tnStudent;
  private ResultsSummaryResponse elSummary;
  private ResultsSummaryResponse tnSummary;
  private ResultsSummaryResponse nonGraduateSummary;

  @BeforeEach
  void setUp() {
    promotionId = UUID.fromString("12345678-1234-1234-1234-123456789012");
    promotion =
        JPromotion.builder()
            .id(promotionId)
            .ref("2025")
            .name("Promotion 2025")
            .entryYear(2025)
            .build();
    elStudent =
        JStudent.builder()
            .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
            .firstname("Grace")
            .lastname("Hopper")
            .std("STD25001")
            .promotion(promotion)
            .build();
    tnStudent =
        JStudent.builder()
            .id(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
            .firstname("Alan")
            .lastname("Turing")
            .std("STD25002")
            .promotion(promotion)
            .build();
    elSummary =
        ResultsSummaryResponse.builder()
            .studentId(elStudent.getId())
            .studentStd("STD25001")
            .overallAverage(new BigDecimal("14.00"))
            .graduate(true)
            .build();
    tnSummary =
        ResultsSummaryResponse.builder()
            .studentId(tnStudent.getId())
            .studentStd("STD25002")
            .overallAverage(new BigDecimal("16.00"))
            .graduate(true)
            .build();
    nonGraduateSummary =
        ResultsSummaryResponse.builder()
            .studentId(tnStudent.getId())
            .studentStd("STD25002")
            .overallAverage(new BigDecimal("8.00"))
            .graduate(false)
            .build();
  }

  @Test
  void getGraduates_throws_not_found_when_promotion_missing() {
    given(promotionRepository.existsById(promotionId)).willReturn(false);

    var ex = assertThrows(ResourceNotFoundException.class, () -> service.getGraduates(promotionId));

    assertEquals("Promotion with id:" + promotionId + " not found.", ex.getMessage());
  }

  @Test
  void getGraduates_returns_only_graduates_grouped_by_track_with_ranks() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent, tnStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent, tnStudent)))
        .willReturn(Map.of(elStudent.getId(), elSummary, tnStudent.getId(), tnSummary));
    given(resultService.currentTracks(List.of(elStudent, tnStudent)))
        .willReturn(Map.of(elStudent.getId(), Track.EL, tnStudent.getId(), Track.TN));

    var result = service.getGraduates(promotionId);

    assertEquals(2, result.size());
    assertEquals(1, result.get(0).rank());
    assertEquals(Track.EL, result.get(0).track());
    assertEquals("STD25001", result.get(0).std());
    assertEquals(1, result.get(1).rank());
    assertEquals(Track.TN, result.get(1).track());
    assertEquals("STD25002", result.get(1).std());
  }

  @Test
  void getGraduates_filters_out_non_graduates() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), nonGraduateSummary));
    given(resultService.currentTracks(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), Track.EL));

    var result = service.getGraduates(promotionId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_returns_empty_when_no_students() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of());
    given(resultService.computeResultsSummaries(List.of())).willReturn(Map.of());
    given(resultService.currentTracks(List.of())).willReturn(Map.of());

    var result = service.getGraduates(promotionId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_treats_null_track_as_el_bucket() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), elSummary));
    var tracks = new java.util.HashMap<UUID, Track>();
    tracks.put(elStudent.getId(), null);
    given(resultService.currentTracks(List.of(elStudent))).willReturn(tracks);

    var result = service.getGraduates(promotionId);

    assertEquals(1, result.size());
    assertNull(result.get(0).track());
  }

  @Test
  void isAcrossThreeYears_returns_true_when_any_graduate() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent, tnStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent, tnStudent)))
        .willReturn(Map.of(elStudent.getId(), elSummary, tnStudent.getId(), nonGraduateSummary));

    assertTrue(service.isAcrossThreeYears(promotionId));
  }

  @Test
  void isAcrossThreeYears_returns_false_when_no_graduate() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), nonGraduateSummary));

    assertFalse(service.isAcrossThreeYears(promotionId));
  }

  @Test
  void isAcrossThreeYears_throws_not_found_when_promotion_missing() {
    given(promotionRepository.existsById(promotionId)).willReturn(false);

    assertThrows(ResourceNotFoundException.class, () -> service.isAcrossThreeYears(promotionId));
  }

  @Test
  void buildXlsx_returns_non_empty_workbook_bytes() {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), elSummary));
    given(resultService.currentTracks(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), Track.EL));

    byte[] bytes = service.buildXlsx(promotionId);

    assertNotNull(bytes);
    assertTrue(bytes.length > 0);
  }

  @Test
  void buildFileName_returns_name_from_promotion_ref() {
    given(promotionRepository.findById(promotionId)).willReturn(Optional.of(promotion));

    var name = service.buildFileName(promotionId);

    assertTrue(name.startsWith("2025 - GRADUATE LIST - "));
    assertTrue(name.endsWith(".xlsx"));
  }

  @Test
  void buildFileName_throws_not_found_when_promotion_missing() {
    given(promotionRepository.findById(promotionId)).willReturn(Optional.empty());

    var ex =
        assertThrows(ResourceNotFoundException.class, () -> service.buildFileName(promotionId));

    assertEquals("Promotion with id:" + promotionId + " not found.", ex.getMessage());
  }

  @Test
  void export_uploads_workbook_and_returns_presigned_url() throws Exception {
    given(promotionRepository.existsById(promotionId)).willReturn(true);
    given(promotionRepository.findById(promotionId)).willReturn(Optional.of(promotion));
    given(studentRepository.findByPromotionIdOrderByLastnameAscFirstnameAsc(promotionId))
        .willReturn(List.of(elStudent));
    given(resultService.computeResultsSummaries(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), elSummary));
    given(resultService.currentTracks(List.of(elStudent)))
        .willReturn(Map.of(elStudent.getId(), Track.EL));
    var url = new URL("https://bucket.s3/graduates/2025.xlsx");
    given(bucketComponent.presign(anyString(), any(Duration.class))).willReturn(url);

    var result = service.export(promotionId);

    assertEquals("https://bucket.s3/graduates/2025.xlsx", result);
    then(bucketComponent).should().upload(any(java.io.File.class), anyString());
  }

  @Test
  void export_does_not_upload_when_promotion_missing() {
    given(promotionRepository.existsById(promotionId)).willReturn(false);

    assertThrows(ResourceNotFoundException.class, () -> service.export(promotionId));
    then(bucketComponent).should(never()).upload(any(), anyString());
  }
}
