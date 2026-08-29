package com.kds.backend.members.application;

public record MemberImportMapping(
        String emailColumn,
        String firstNameColumn,
        String lastNameColumn,
        String phoneColumn) {
}
