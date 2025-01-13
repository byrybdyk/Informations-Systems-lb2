package com.byrybdyk.lb1.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "import_history")
public class ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int addedObjectsCount;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(nullable = false)
    private Boolean isSuccessful;

    @PrePersist
    protected void onCreate() {
        this.creationDate = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getAddedObjectsCount() {
        return addedObjectsCount;
    }

    public void setAddedObjectsCount(int addedObjectsCount) {
        this.addedObjectsCount = addedObjectsCount;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Boolean getIsSuccessful() {
        return isSuccessful;
    }

    public void setIsSuccessful(Boolean successful) {
        isSuccessful = successful;
    }
}
