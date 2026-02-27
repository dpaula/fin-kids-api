package br.com.autevia.finkidsapi.domain.entity;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "account_users")
public class AccountUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_role", nullable = false, length = 20)
    private UserRole profileRole;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountUser() {
    }

    public AccountUser(Account account, AppUser user, UserRole profileRole) {
        this.account = account;
        this.user = user;
        this.profileRole = profileRole;
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

    public void setAccount(Account account) {
        this.account = account;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public UserRole getProfileRole() {
        return profileRole;
    }

    public void setProfileRole(UserRole profileRole) {
        this.profileRole = profileRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
