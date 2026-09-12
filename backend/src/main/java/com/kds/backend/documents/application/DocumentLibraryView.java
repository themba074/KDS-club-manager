package com.kds.backend.documents.application;

import java.util.List;

public record DocumentLibraryView(List<DocumentView> documents, List<RoleOption> visibilityRoles, boolean canManage) {
    public record RoleOption(String code, String name) {}
}
