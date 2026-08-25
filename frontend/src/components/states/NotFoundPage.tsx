import { SearchX } from "lucide-react"
import { Link } from "react-router-dom"

import { EmptyState } from "@/components/states/EmptyState"

export function NotFoundPage() {
  return (
    <EmptyState
      icon={SearchX}
      title="Page not found"
      description="The page you requested does not exist or may have moved."
      action={
        <Link
          to="/"
          className="inline-flex h-8 items-center justify-center rounded-lg bg-primary px-2.5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/80 focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          Return to dashboard
        </Link>
      }
    />
  )
}
