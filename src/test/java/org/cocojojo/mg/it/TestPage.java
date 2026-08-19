package org.cocojojo.mg.it;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Minimal view of a Spring Data {@code Page} JSON response, used by integration tests. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestPage<T>(List<T> content, long totalElements, int totalPages, int number) {}
