package com.example.michelin.repository;

import com.example.michelin.entity.EvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {

    List<EvaluationEntity> findByAuthorId(String authorId);
}
