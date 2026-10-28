package com.apiwatch.repository;

import com.apiwatch.entity.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {

    List<HealthCheck> findByEndpointIdOrderByCheckedAtDesc(Long endpointId);

    List<HealthCheck> findTop50ByEndpointIdOrderByCheckedAtDesc(Long endpointId);

    long countByEndpointId(Long endpointId);
}
