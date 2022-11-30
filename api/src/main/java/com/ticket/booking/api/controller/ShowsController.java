package com.ticket.booking.api.controller;

import com.ticket.booking.api.dto.Reservation;
import com.ticket.booking.api.dto.SeatAllocation;
import com.ticket.booking.api.dto.UserBasedAllocation;
import com.ticket.booking.api.service.ShowsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@Slf4j
public class ShowsController {

    private final ShowsService showsService;

    @GetMapping("allocation/{allocationId}")
    public ResponseEntity<UserBasedAllocation> getUserBasedAllocation(@PathVariable("allocationId") String allocationId) {
        return ResponseEntity.ok(showsService.getUserBasedAllocation(allocationId));
    }

    @GetMapping("show/{showId}")
    public ResponseEntity<SeatAllocation> listSeats(@PathVariable("showId") String showId,
                                                    @RequestHeader String userId) {
        return ResponseEntity.ok(showsService.listAllSeats(showId, userId));
    }

    @PutMapping("show/{showId}/block")
    public ResponseEntity<Void> blockSeats(@PathVariable("showId") String showId,
                                           @RequestHeader String userId,
                                           @RequestBody Reservation reservation) {
        log.debug("Request to block seats for show id = {}", showId);
        String allocationId = showsService.domainBlockSeats(userId, showId, reservation.getSeats());
        return ResponseEntity.noContent()
                .header("allocationId", allocationId)
                .build();
    }

    @PutMapping("show/{showId}/unblock")
    public ResponseEntity<Void> unblockSeats(@PathVariable("showId") String showId,
                                             @RequestHeader String userId,
                                             @RequestBody Reservation reservation) {
        showsService.unblockSeats(userId, showId, reservation.getSeats());
        return ResponseEntity.noContent()
                .build();
    }


    @PutMapping("show/{showId}/book")
    public ResponseEntity<Void> book(@PathVariable("showId") String showId,
                                     @RequestHeader String userId,
                                     @RequestBody Reservation reservation) {
        log.debug("Request to book seats for show id = {}", showId);
        showsService.book(userId, showId, reservation.getSeats());
        return ResponseEntity.noContent()
                .build();
    }
}
