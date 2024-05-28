package com.codeages.termiusplus.biz.storage.repository;

import com.codeages.termiusplus.biz.storage.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long>, QuerydslPredicateExecutor<File> {

    Optional<File> getByUuid(String uuid);

    List<File> findAllByUuidIn(Collection<String> uuid);
}
