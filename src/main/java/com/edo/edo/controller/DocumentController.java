package com.edo.edo.controller;

import com.edo.edo.models.Document;
import com.edo.edo.models.User;
import com.edo.edo.repository.DocumentRepository;
import com.edo.edo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentController {

        private final DocumentRepository documentRepository;
        private final UserRepository userRepository;

        @PostMapping("/upload")
        public ResponseEntity<Document> uploadDocument(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("name") String name,
                        @RequestParam("tag") String tag,
                        @RequestParam("recipient") String recipient,
                        Principal principal) throws IOException {
                var recipientUser = userRepository.findByEmail(recipient.trim())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Пользователь с email '" + recipient + "' не найден"));

                Document doc = Document.builder()
                                .name(name)
                                .tag(tag)
                                .recipient(recipientUser)
                                .sender(userRepository.findByEmail(principal.getName().trim()).orElseThrow())
                                .status("На рассмотрении")
                                .created(LocalDateTime.now())
                                .modified(LocalDateTime.now())
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .fileData(file.getBytes())
                                .build();

                return ResponseEntity.ok(documentRepository.save(doc));
        }

        @GetMapping("/{id}/download")
        public ResponseEntity<byte[]> download(@PathVariable Long id) {
                Document doc = documentRepository.findById(id).orElseThrow();

                System.out.println("📥 Запрос на download: " + doc.getFileName());

                return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + doc.getFileName() + "\"")
                                .header("Content-Type", doc.getFileType())
                                .body(doc.getFileData());
        }

        

        @GetMapping("/{id}")
        public ResponseEntity<Document> getDocumentById(@PathVariable Long id, Principal principal) {
                Document doc = documentRepository.findById(id).orElseThrow();

                return ResponseEntity.ok(doc);
        }

        @PutMapping("/{id}/approve")
        public ResponseEntity<Document> approveDocument(@PathVariable Long id, Principal principal) {
                Document doc = documentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Документ не найден с id=" + id));

                doc.setStatus("Утвержден");
                doc.setModified(LocalDateTime.now());
                Document updated = documentRepository.save(doc);
                return ResponseEntity.ok(updated);
        }

        @GetMapping("/inbox")
        public ResponseEntity<List<Document>> getReceived(Principal principal) {
                User me = userRepository.findByEmail(principal.getName()).orElseThrow();
                return ResponseEntity.ok(documentRepository.findByRecipient(me));
        }

        @GetMapping("/sent")
        public ResponseEntity<List<Document>> getSent(Principal principal) {
                User me = userRepository.findByEmail(principal.getName()).orElseThrow();
                return ResponseEntity.ok(documentRepository.findBySender(me));
        }

        @GetMapping("/approved")
        public ResponseEntity<List<Document>> getApproved(Principal principal) {
                User me = userRepository.findByEmail(principal.getName()).orElseThrow();
                List<Document> approvedReceived = documentRepository.findByRecipientAndStatus(me, "Утвержден");
                List<Document> approvedSent = documentRepository.findBySenderAndStatus(me, "Утвержден");
                approvedReceived.addAll(approvedSent);
                return ResponseEntity.ok(approvedReceived);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Principal principal) {
                Document doc = documentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Документ не найден с id=" + id));

                User me = userRepository.findByEmail(principal.getName()).orElseThrow();
                if (!doc.getSender().getId().equals(me.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                documentRepository.delete(doc);
                return ResponseEntity.noContent().build();
        }
}
