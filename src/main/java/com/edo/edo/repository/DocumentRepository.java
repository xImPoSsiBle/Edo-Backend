package com.edo.edo.repository;

import com.edo.edo.models.Document;
import com.edo.edo.models.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByRecipient(User recipient);

    List<Document> findBySender(User sender);

    List<Document> findByStatus(String status);

    List<Document> findByRecipientAndStatus(User recipient, String status);

    List<Document> findBySenderAndStatus(User sender, String status);
}
