package com.byrybdyk.lb1.repository;

import com.byrybdyk.lb1.model.ImportHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {

    List<ImportHistory> findByUserUsername(String username, Sort creationDate);

    List<ImportHistory> findAll(Sort sort);
}
