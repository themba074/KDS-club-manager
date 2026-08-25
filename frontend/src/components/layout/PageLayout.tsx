import type { ReactNode } from "react"

export type PageLayoutProps = {
  title: string
  description: string
  actions?: ReactNode
  children: ReactNode
}

export function PageLayout({ title, description, actions, children }: PageLayoutProps) {
  return (
    <section className="space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="max-w-3xl">
          <h1 className="font-heading text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">{description}</p>
        </div>
        {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
      </header>
      {children}
    </section>
  )
}
