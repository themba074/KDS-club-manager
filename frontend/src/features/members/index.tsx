import { Users } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function MembersPage() {
  return (
    <ModulePlaceholder
      title="Members"
      description="Manage invitations, member profiles, roles, and membership status."
      emptyTitle="No members to show yet"
      emptyDescription="Your club directory will appear here when member onboarding is available."
      icon={Users}
    />
  )
}
