package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.ExpenseCategoryChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryChangeRepository extends JpaRepository<ExpenseCategoryChange, Long> {
}
