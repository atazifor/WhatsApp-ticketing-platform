package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    // Add custom query methods here if needed
}
