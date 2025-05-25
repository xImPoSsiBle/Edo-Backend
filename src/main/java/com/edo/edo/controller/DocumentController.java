package com.edo.edo.controller;

import com.edo.edo.models.Document;
import com.edo.edo.models.User;
import com.edo.edo.repository.DocumentRepository;
import com.edo.edo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

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
                System.out.println("📨 Получатель: " + '[' + recipient + ']');
                userRepository.findAll().forEach(user -> System.out.println("👤 User: " + user.getEmail()));

                Document doc = Document.builder()
                                .name(name)
                                .tag(tag)
                                .recipient(userRepository.findByEmail(recipient.trim()).orElseThrow(
                                                () -> new RuntimeException("❌ Пользователь с email '" + recipient
                                                                + "' не найден")))
                                .sender(userRepository.findByEmail(principal.getName().trim()).orElseThrow())
                                .status("Черновик")
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

        @GetMapping("/getInboxDocs")
        public ResponseEntity<List<Document>> getInboxDocuments(Principal principal) {
                System.out.println("📥 Запрос на inbox от: " + principal.getName());
                var recipient = userRepository.findByEmail(principal.getName().trim())
                                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

                List<Document> inboxDocs = documentRepository.findByRecipient(recipient);
                return ResponseEntity.ok(inboxDocs);
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
}
