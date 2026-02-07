package com.onidza.backend.model.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Generated
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseKafkaEntity implements Serializable {

    @Id
    private UUID uuid;

    @CreatedDate
    private Instant createdAt;

    /**
     we can use it for validate our IN_PROGRESS stale tasks like in query:
     WHERE status = IN_PROGRESS and updatedAt <= Instant.now() - delay
    */
    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Integer version;
}
