package com.kds.backend.meetings.application;

public record MinutesAttachment(String fileName,String contentType,byte[] content) {}
