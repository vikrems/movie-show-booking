package com.ticket.booking.api.controller;

import com.ticket.booking.api.dto.BookingAllocation;
import com.ticket.booking.api.dto.SeatAllocation;
import com.ticket.booking.api.service.ShowsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("show/{showId}")
@RequiredArgsConstructor
@Slf4j
public class ShowsController {

    private final ShowsService showsService;

    @GetMapping
    public ResponseEntity<SeatAllocation> listSeats(@PathVariable("showId") String showId,
                                                    @RequestHeader String userId) {
        return ResponseEntity.ok(showsService.getSeatAllocation(showId, userId));
    }

    @PutMapping("block")
    public ResponseEntity<Void> blockSeats(@PathVariable("showId") String showId,
                                           @RequestHeader String userId,
                                           @RequestBody BookingAllocation allocation) {
        log.debug("Request to block seats for show id = {}", showId);
        showsService.blockSeats(userId, showId, allocation.getSeats());
        return ResponseEntity.noContent()
                .build();
    }

    @PutMapping("unblock")
    public ResponseEntity<Void> unblockSeats(@PathVariable("showId") String showId,
                                             @RequestHeader String userId,
                                             @RequestBody BookingAllocation allocation) {
        showsService.unblockSeats(userId, showId, allocation.getSeats());
        return ResponseEntity.noContent()
                .build();
    }


    @PutMapping("book")
    public ResponseEntity<Void> book(@PathVariable("showId") String showId,
                                             @RequestHeader String userId,
                                             @RequestBody BookingAllocation allocation) {
        log.debug("Request to book seats for show id = {}", showId);
        showsService.book(userId, showId, allocation.getSeats());
        return ResponseEntity.noContent()
                .build();
    }
}
