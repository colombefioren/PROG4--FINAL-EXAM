package org.cocojojo.mg.service.event;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.internet.InternetAddress;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cocojojo.mg.endpoint.event.model.TranscriptRequested;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.YearlyResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.file.bucket.BucketComponent;
import org.cocojojo.mg.mail.Email;
import org.cocojojo.mg.mail.Mailer;
import org.cocojojo.mg.model.enums.ResultStatus;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.service.ResultService;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptRequestedService implements Consumer<TranscriptRequested> {

  private static final String TEMPLATE = "transcript";
  private static final String EMAIL_TEMPLATE = "transcript-email";
  private static final Duration LINK_VALIDITY = Duration.ofDays(7);

  private final StudentRepository studentRepository;
  private final ResultService resultService;
  private final ITemplateEngine templateEngine;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(TranscriptRequested event) {
    var student = findStudent(event.getStudentId());
    StudentLevel level = StudentLevel.valueOf(event.getLevel());
    YearlyResultResponse yearly = resultService.computeYearlyResult(student.getId(), level);

    String html = render(student, yearly);
    byte[] pdf = renderPdf(html);

    String key = "transcripts/" + student.getStd() + "/" + Instant.now().toEpochMilli() + ".pdf";
    var tempFile = File.createTempFile("transcript-" + student.getStd() + "-", ".pdf");
    Files.write(tempFile.toPath(), pdf);
    bucketComponent.upload(tempFile, key);
    var link = bucketComponent.presign(key, LINK_VALIDITY);

    var htmlBody = renderEmailBody(student, yearly, link);

    mailer.accept(
        new Email(
            new InternetAddress(student.getEmail()),
            List.of(),
            List.of(),
            "Your HEI grade transcript",
            htmlBody,
            List.of()));
    log.info("Transcript sent to student {}", student.getStd());
  }

  private JStudent findStudent(String studentId) {
    return studentRepository
        .findById(UUID.fromString(studentId))
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id: " + studentId + " not found."));
  }

  private String renderEmailBody(JStudent student, YearlyResultResponse yearly, URL link) {
    var context = new Context();
    context.setVariable("title", "Your HEI grade transcript");
    context.setVariable("studentFirstname", student.getFirstname());
    context.setVariable("studentName", student.getFirstname() + " " + student.getLastname());
    context.setVariable("stdRef", student.getStd());
    context.setVariable("level", yearly.level());
    context.setVariable(
        "overallAverage",
        yearly.overallAverage() == null ? "-" : yearly.overallAverage().toString());
    context.setVariable(
        "complete", yearly.status() == ResultStatus.COMPLETED ? "Complete" : "Provisional");
    context.setVariable("earnedCredits", yearly.earnedCredits());
    context.setVariable("totalCredits", yearly.totalCredits());
    context.setVariable("validityDays", LINK_VALIDITY.toDays());
    context.setVariable("link", link);
    context.setVariable(
        "generatedAt", DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.now()));
    return templateEngine.process(EMAIL_TEMPLATE, context);
  }

  private String render(JStudent student, YearlyResultResponse yearly) {
    var context = new Context();
    context.setVariable("title", "Grade transcript - " + yearly.level());
    context.setVariable("studentName", student.getFirstname() + " " + student.getLastname());
    context.setVariable("stdRef", student.getStd());
    context.setVariable(
        "generatedAt", DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(LocalDate.now()));
    context.setVariable(
        "overallAverage",
        yearly.overallAverage() == null ? "-" : yearly.overallAverage().toString());
    context.setVariable(
        "complete", yearly.status() == ResultStatus.COMPLETED ? "Complete" : "Provisional");
    context.setVariable("earnedCredits", yearly.earnedCredits());
    context.setVariable("totalCredits", yearly.totalCredits());
    context.setVariable("courses", yearly.courses().stream().map(this::toCourseView).toList());
    return templateEngine.process(TEMPLATE, context);
  }

  private Map<String, Object> toCourseView(CourseResultResponse course) {
    Map<String, Object> map = new HashMap<>();
    map.put("code", course.courseCode());
    map.put("name", course.courseName());
    map.put("credits", course.credits());
    map.put("average", course.average() == null ? "-" : course.average().toString());
    map.put(
        "validated",
        !Boolean.TRUE.equals(course.graded())
            ? "Not graded yet"
            : Boolean.TRUE.equals(course.passed()) ? "Passed" : "Not passed");
    return map;
  }

  private byte[] renderPdf(String html) throws Exception {
    var outputStream = new ByteArrayOutputStream();
    var builder = new PdfRendererBuilder();
    builder.useFastMode();
    builder.withHtmlContent(html, null);
    builder.toStream(outputStream);
    builder.run();
    return outputStream.toByteArray();
  }
}
