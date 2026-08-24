import { Vote } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function VotingPage() {
  return (
    <ModulePlaceholder
      title="Voting"
      description="Create motions, manage voting windows, and publish transparent results."
      emptyTitle="No motions are open"
      emptyDescription="Current and completed club votes will appear here."
      icon={Vote}
    />
  )
}
