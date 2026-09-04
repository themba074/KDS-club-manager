package com.kds.backend.meetings.application;

import com.kds.backend.meetings.domain.MeetingRsvpEntity.Response;

public record RsvpView(Response response, Counts counts) {
    public record Counts(long yes,long no,long maybe) {}
}
