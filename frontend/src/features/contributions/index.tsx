import { HandCoins } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function ContributionsPage() {
  return (
    <ModulePlaceholder
      title="Contributions"
      description="Track contribution schedules, payments, balances, and member ledgers."
      emptyTitle="No contribution activity yet"
      emptyDescription="Schedules and payment records will appear here once the contributions module is configured."
      icon={HandCoins}
    />
  )
}
