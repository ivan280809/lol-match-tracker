package com.loltracker.app.ops;

import java.time.Instant;

public record PollLease(String owner, Instant lockedUntil) {}
