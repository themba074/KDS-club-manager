import { ChartNoAxesCombined } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function ReportsPage() {
  return (
    <ModulePlaceholder
      title="Reports"
      description="Review and export club membership, contribution, meeting, and voting information."
      emptyTitle="No reports available yet"
      emptyDescription="Reports will become available as each club module begins collecting data."
      icon={ChartNoAxesCombined}
    />
  )
}
