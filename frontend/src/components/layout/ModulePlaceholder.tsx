import type { LucideIcon } from "lucide-react"

import { PageLayout } from "@/components/layout/PageLayout"
import { EmptyState } from "@/components/states/EmptyState"

type ModulePlaceholderProps = {
  title: string
  description: string
  emptyTitle: string
  emptyDescription: string
  icon: LucideIcon
}

export function ModulePlaceholder({
  title,
  description,
  emptyTitle,
  emptyDescription,
  icon,
}: ModulePlaceholderProps) {
  return (
    <PageLayout title={title} description={description}>
      <EmptyState icon={icon} title={emptyTitle} description={emptyDescription} />
    </PageLayout>
  )
}
