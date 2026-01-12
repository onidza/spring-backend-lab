package com.onidza.backend.repository;

import com.onidza.backend.model.entity.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @EntityGraph(attributePaths = "client")
    Slice<Profile> findBy(Pageable pageable);
}
