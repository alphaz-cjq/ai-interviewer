package com.cjq.service;

import com.cjq.pojo.DTO.JobPositionRequest;
import com.cjq.pojo.PO.JobPosition;

import java.util.List;

public interface JobPositionService {
    JobPosition create(JobPositionRequest request);
    JobPosition update(Long id, JobPositionRequest request);
    List<JobPosition> listAll();
    JobPosition getById(Long id);
    void delete(Long id);
}
