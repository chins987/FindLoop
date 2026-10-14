package com.findloop.item;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_reports", indexes = {
        @Index(name = "idx_reports_status_type_created", columnList = "status,type,created_at"),
        @Index(name = "idx_reports_lat_lon", columnList = "latitude,longitude")
})
public class ItemReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    @JsonIgnore
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemType type;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 160)
    private String location;

    @Column(length = 500)
    private String locationDetails;

    @Column(length = 240)
    @JsonIgnore
    private String ownershipQuestion;

    @Column(length = 100)
    @JsonIgnore
    private String ownershipAnswerHash;

    @Column
    @JsonIgnore
    private Double latitude;

    @Column
    @JsonIgnore
    private Double longitude;

    // When the item was lost/found, not when the report was submitted.
    @Column(nullable = false)
    private LocalDateTime eventDateTime;

    @Column(nullable = false, length = 40)
    private String status = "OPEN";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ItemReport() {
    }

    public ItemReport(ItemType type, String title, String description, String category,
            String location, LocalDateTime eventDateTime) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.eventDateTime = eventDateTime;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public ItemType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public String getLocationDetails() {
        return locationDetails;
    }

    public String getOwnershipQuestion() { return ownershipQuestion; }
    public String getOwnershipAnswerHash() { return ownershipAnswerHash; }
    public void setOwnershipCheck(String question, String answerHash) {
        this.ownershipQuestion = question;
        this.ownershipAnswerHash = answerHash;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
