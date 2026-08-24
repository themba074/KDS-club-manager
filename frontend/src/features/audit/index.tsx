import { Scale } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function AuditPage() {
  return (
    <ModulePlaceholder
      title="Audit log"
      description="Review an immutable history of important financial and governance actions."
      emptyTitle="No audit activity yet"
      emptyDescription="Recorded actions will appear here with their actor and timestamp."
      icon={Scale}
    />
  )
}
