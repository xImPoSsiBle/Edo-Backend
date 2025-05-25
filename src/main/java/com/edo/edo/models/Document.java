package com.edo.edo.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String tag;
    private String status;

    private String fileName;
    private String fileType;

    @Column(name = "file_data")
    private byte[] fileData;

    private LocalDateTime created;
    private LocalDateTime modified;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User recipient;
}
