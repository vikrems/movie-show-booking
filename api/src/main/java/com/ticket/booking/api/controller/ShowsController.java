package com.ticket.booking.api.controller;

import com.ticket.booking.api.dto.BookingAllocation;
import com.ticket.booking.api.dto.SeatAllocation;
import com.ticket.booking.api.service.ShowsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("show/{showId}")
@RequiredArgsConstructor
public class ShowsController {

    private final ShowsService showsService;

    @GetMapping
    public ResponseEntity<SeatAllocation> listSeats(@PathVariable("showId") String showId) {
        return ResponseEntity.ok(showsService.getSeatAllocation(showId));
    }

    @PutMapping("block")
    public ResponseEntity<Void> blockSeats(@PathVariable("showId") String showId,
                                           @RequestHeader String userId,
                                           @RequestBody BookingAllocation allocation) {
        showsService.blockSeats(userId, showId, allocation.getSeats());
        return ResponseEntity.noContent()
                .build();
    }

    @PutMapping("unblock")
    public ResponseEntity<Void> unblockSeats(@PathVariable("showId") String showId,
                                             @RequestHeader String user,
                                             @RequestBody BookingAllocation allocation) {
        return null;
    }
}
