package io.poddeck.core.mfa;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MultiFactorSubmissionRepository extends DatabaseRepository<MultiFactorSubmission, UUID> {
}