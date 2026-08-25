import type { LucideIcon } from "lucide-react"
import type { ReactNode } from "react"

import { Card, CardContent } from "@/components/ui/card"

type EmptyStateProps = {
  icon: LucideIcon
  title: string
  description: string
  action?: ReactNode
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <Card className="border-dashed bg-card/60 py-12 shadow-none sm:py-16">
      <CardContent className="mx-auto flex max-w-md flex-col items-center text-center">
        <span className="mb-4 grid size-12 place-items-center rounded-2xl bg-accent text-accent-foreground">
          <Icon className="size-6" aria-hidden="true" />
        </span>
        <h2 className="text-base font-semibold">{title}</h2>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">{description}</p>
        {action && <div className="mt-5">{action}</div>}
      </CardContent>
    </Card>
  )
}
