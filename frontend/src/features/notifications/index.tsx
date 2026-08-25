import { Bell } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function NotificationsPage() {
  return (
    <ModulePlaceholder
      title="Notifications"
      description="Stay informed about payments, meetings, votes, and published records."
      emptyTitle="You're all caught up"
      emptyDescription="New club notifications will be collected here."
      icon={Bell}
    />
  )
}
