import { CircleAlert } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"

type ErrorStateProps = {
  title?: string
  description?: string
  onRetry?: () => void
}

export function ErrorState({
  title = "We couldn't load this content",
  description = "Please try again. If the problem continues, contact your club administrator.",
  onRetry,
}: ErrorStateProps) {
  return (
    <Card className="border-destructive/30 bg-destructive/5 py-10 shadow-none" role="alert">
      <CardContent className="mx-auto flex max-w-md flex-col items-center text-center">
        <CircleAlert className="size-8 text-destructive" aria-hidden="true" />
        <h2 className="mt-4 text-base font-semibold">{title}</h2>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">{description}</p>
        {onRetry && (
          <Button variant="outline" className="mt-5" onClick={onRetry}>
            Try again
          </Button>
        )}
      </CardContent>
    </Card>
  )
}
