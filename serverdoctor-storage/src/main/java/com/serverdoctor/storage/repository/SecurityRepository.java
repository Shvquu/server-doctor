package com.serverdoctor.storage.repository;

import com.serverdoctor.common.model.SecurityRisk;

import java.time.Instant;
import java.util.List;

public interface SecurityRepository {
    void save(Instant at, SecurityRisk risk);
    List<SecurityRisk> recent(int limit);
}
