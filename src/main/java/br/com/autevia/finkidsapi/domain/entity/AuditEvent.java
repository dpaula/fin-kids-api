package br.com.autevia.finkidsapi.domain.entity;

import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_events_account_created_at", columnList = "account_id,created_at"),
                @Index(name = "idx_audit_events_actor_email_created_at", columnList = "actor_email,created_at"),
                @Index(name = "idx_audit_events_action_type_created_at", columnList = "action_type,created_at")
        }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actorUser;

    @Column(name = "actor_email", nullable = false, length = 180)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_global_role", length = 20)
    private UserRole actorGlobalRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 40)
    private AuditResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "payload_summary", columnDefinition = "TEXT")
    private String payloadSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            Account account,
            AppUser actorUser,
            String actorEmail,
            UserRole actorGlobalRole,
            AuditActionType actionType,
            AuditResourceType resourceType,
            Long resourceId,
            String payloadSummary
    ) {
        this.account = account;
        this.actorUser = actorUser;
        this.actorEmail = actorEmail;
        this.actorGlobalRole = actorGlobalRole;
        this.actionType = actionType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payloadSummary = payloadSummary;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public AppUser getActorUser() {
        return actorUser;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public UserRole getActorGlobalRole() {
        return actorGlobalRole;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public AuditResourceType getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getPayloadSummary() {
        return payloadSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
