package org.cocojojo.mg.mapper;

import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupFlowResponse;
import org.cocojojo.mg.model.GroupFlow;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GroupFlowMapper {

  private final StudentMapper studentMapper;
  private final GroupMapper groupMapper;

  public GroupFlow toModel(JGroupFlow entity) {
    return GroupFlow.builder()
        .id(entity.getId())
        .student(studentMapper.toSummary(entity.getStudent()))
        .group(groupMapper.toModel(entity.getGroup()))
        .groupFlowType(entity.getGroupFlowType())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  public GroupFlowResponse toResponse(GroupFlow model) {
    return GroupFlowResponse.builder()
        .id(model.id())
        .studentId(model.student().id())
        .studentStd(model.student().std())
        .groupId(model.group().id())
        .groupRef(model.group().ref())
        .groupFlowType(model.groupFlowType())
        .createdAt(model.createdAt())
        .build();
  }
}
