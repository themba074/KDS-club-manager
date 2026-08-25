export function LoadingState({ label = "Loading content" }: { label?: string }) {
  return (
    <div className="space-y-4" role="status" aria-label={label}>
      <div className="h-28 animate-pulse rounded-xl bg-muted" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {[0, 1, 2].map((item) => (
          <div key={item} className="h-36 animate-pulse rounded-xl bg-muted" />
        ))}
      </div>
      <span className="sr-only">{label}</span>
    </div>
  )
}
