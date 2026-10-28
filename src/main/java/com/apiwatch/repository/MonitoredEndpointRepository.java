package com.apiwatch.repository;

import com.apiwatch.entity.MonitoredEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredEndpointRepository extends JpaRepository<MonitoredEndpoint, Long> {

    List<MonitoredEndpoint> findByUserId(Long userId);

    Optional<MonitoredEndpoint> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Loads all enabled endpoints together with their owner User in a single JOIN FETCH SQL query.
     * This prevents LazyInitializationException when the scheduler accesses endpoint.getUser()
     * outside the original transaction that loaded the endpoint.
     */
    @Query("SELECT e FROM MonitoredEndpoint e JOIN FETCH e.user WHERE e.enabled = true")
    List<MonitoredEndpoint> findAllEnabledWithUser();
}
