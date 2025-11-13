package io.poddeck.core.member;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MemberPasswordResetRepository extends DatabaseRepository<MemberPasswordReset, UUID> {
}