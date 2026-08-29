package com.kds.backend.members.api;

import com.kds.backend.members.application.MemberImportMapping;
import jakarta.validation.constraints.NotBlank;

public final class MemberImportDtos {
    private MemberImportDtos() {}

    public static class ColumnMapping {
        @NotBlank private String emailColumn;
        @NotBlank private String firstNameColumn;
        @NotBlank private String lastNameColumn;
        private String phoneColumn;

        public String getEmailColumn() { return emailColumn; }
        public void setEmailColumn(String emailColumn) { this.emailColumn = emailColumn; }
        public String getFirstNameColumn() { return firstNameColumn; }
        public void setFirstNameColumn(String firstNameColumn) { this.firstNameColumn = firstNameColumn; }
        public String getLastNameColumn() { return lastNameColumn; }
        public void setLastNameColumn(String lastNameColumn) { this.lastNameColumn = lastNameColumn; }
        public String getPhoneColumn() { return phoneColumn; }
        public void setPhoneColumn(String phoneColumn) { this.phoneColumn = phoneColumn; }

        public MemberImportMapping toApplication() {
            return new MemberImportMapping(emailColumn, firstNameColumn, lastNameColumn, phoneColumn);
        }
    }
}
