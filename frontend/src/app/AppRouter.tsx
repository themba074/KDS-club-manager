import { BrowserRouter, Route, Routes } from "react-router-dom"

import { AppLayout } from "@/app/AppLayout"
import { NotFoundPage } from "@/components/states/NotFoundPage"
import { AuditPage } from "@/features/audit"
import { ContributionsPage } from "@/features/contributions"
import { DashboardPage } from "@/features/dashboard"
import { DocumentsPage } from "@/features/documents"
import { MeetingsPage } from "@/features/meetings"
import { MembersPage } from "@/features/members"
import { NotificationsPage } from "@/features/notifications"
import { ReportsPage } from "@/features/reports"
import { VotingPage } from "@/features/voting"

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="contributions" element={<ContributionsPage />} />
          <Route path="meetings" element={<MeetingsPage />} />
          <Route path="voting" element={<VotingPage />} />
          <Route path="documents" element={<DocumentsPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="audit" element={<AuditPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
