package com.ridehailing.booking_service.repository;


import com.ridehailing.booking_service.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxMessagingRepository extends JpaRepository<OutboxMessage, Long> {

}
