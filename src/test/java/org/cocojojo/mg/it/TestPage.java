package org.cocojojo.mg.it;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TestPage<T>(List<T> content, long totalElements, int totalPages, int number) {}
