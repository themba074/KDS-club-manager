import { ClipboardCheck } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function MeetingsPage() {
  return (
    <ModulePlaceholder
      title="Meetings"
      description="Schedule meetings, prepare agendas, track RSVPs, and publish minutes."
      emptyTitle="No meetings scheduled"
      emptyDescription="Upcoming and previous club meetings will be organised here."
      icon={ClipboardCheck}
    />
  )
}
