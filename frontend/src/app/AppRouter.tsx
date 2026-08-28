import { BrowserRouter, Route, Routes } from "react-router-dom"

import { AppLayout } from "@/app/AppLayout"
import { NotFoundPage } from "@/components/states/NotFoundPage"
import { AuditPage } from "@/features/audit"
import { ContributionsPage } from "@/features/contributions"
import { DashboardPage } from "@/features/dashboard"
import { DocumentsPage } from "@/features/documents"
import { MeetingsPage } from "@/features/meetings"
import { MembersPage } from "@/features/members"
import { AcceptInvitationPage } from "@/features/members/AcceptInvitationPage"
import { NotificationsPage } from "@/features/notifications"
import { ReportsPage } from "@/features/reports"
import { VotingPage } from "@/features/voting"
import { CredentialsPage, ForgotPasswordPage, ResetPasswordPage } from "@/features/auth/AuthPages"
import { ProtectedRoute } from "@/features/auth/ProtectedRoute"
import { ClubWorkspacePage } from "@/features/clubs/ClubWorkspacePage"
import { RequireClub } from "@/features/clubs/RequireClub"
import { RoleManagement } from "@/features/roles/RoleManagement"
import { PermissionGate } from "@/features/roles/permissions"

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="login" element={<CredentialsPage mode="login" />} />
        <Route path="register" element={<CredentialsPage mode="register" />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route path="accept-invitation" element={<AcceptInvitationPage />} />
        <Route element={<ProtectedRoute />}>
        <Route path="clubs" element={<ClubWorkspacePage />} />
        <Route element={<RequireClub />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="roles" element={<PermissionGate permission="ROLES_READ"><RoleManagement /></PermissionGate>} />
          <Route path="members" element={<PermissionGate permission="MEMBERS_READ"><MembersPage /></PermissionGate>} />
          <Route path="contributions" element={<PermissionGate permission="CONTRIBUTIONS_READ"><ContributionsPage /></PermissionGate>} />
          <Route path="meetings" element={<PermissionGate permission="MEETINGS_READ"><MeetingsPage /></PermissionGate>} />
          <Route path="voting" element={<PermissionGate permission="VOTES_READ"><VotingPage /></PermissionGate>} />
          <Route path="documents" element={<PermissionGate permission="DOCUMENTS_READ"><DocumentsPage /></PermissionGate>} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="audit" element={<PermissionGate permission="AUDIT_READ"><AuditPage /></PermissionGate>} />
          <Route path="reports" element={<PermissionGate permission="REPORTS_READ"><ReportsPage /></PermissionGate>} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
        </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
