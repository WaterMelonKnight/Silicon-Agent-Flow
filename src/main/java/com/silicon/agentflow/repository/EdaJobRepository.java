package com.silicon.agentflow.repository;

import com.silicon.agentflow.entity.EdaJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdaJobRepository extends JpaRepository<EdaJob, Long> {
}
