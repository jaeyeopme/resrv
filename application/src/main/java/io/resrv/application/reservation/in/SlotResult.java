package io.resrv.application.reservation.in;

import java.time.Instant;

public record SlotResult(Instant startAt, Instant endAt) {}
