package com.RRBank.banking.repository;

import com.RRBank.banking.entity.AccountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRequestRepository extends JpaRepository<AccountRequest, UUID> {

    List<AccountRequest> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AccountRequest> findByStatusOrderByCreatedAtAsc(AccountRequest.RequestStatus status);

    Page<AccountRequest> findByStatusOrderByCreatedAtAsc(AccountRequest.RequestStatus status, Pageable pageable);

    @Query("SELECT r FROM AccountRequest r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<AccountRequest> findPendingRequests();

    @Query("SELECT r FROM AccountRequest r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    Page<AccountRequest> findPendingRequests(Pageable pageable);

    @Query("SELECT COUNT(r) FROM AccountRequest r WHERE r.status = 'PENDING'")
    long countPendingRequests();

    List<AccountRequest> findByReviewedByOrderByReviewedAtDesc(String adminId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM AccountRequest r " +
           "WHERE r.userId = :userId AND r.accountType = :type AND r.status = 'PENDING'")
    boolean hasPendingRequest(
        @Param("userId") String userId,
        @Param("type") com.RRBank.banking.entity.Account.AccountType type
    );
}
