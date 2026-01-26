package org.example.homedatazip.email.repository;

import org.example.homedatazip.email.entity.EmailAuth;
import org.springframework.data.repository.CrudRepository;

public interface EmailAuthRedisRepository extends CrudRepository<EmailAuth, String> {
}
