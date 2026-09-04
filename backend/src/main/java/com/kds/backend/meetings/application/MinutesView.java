package com.kds.backend.meetings.application;

import java.time.Instant;
import java.util.UUID;

public record MinutesView(UUID id,long version,String body,String attachmentName,Long attachmentSize,Instant publishedAt,boolean published) {}
