import { CalendarDays, HandCoins, TrendingUp, Users } from "lucide-react"

import { PageLayout } from "@/components/layout/PageLayout"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

const summaryCards = [
  { label: "Active members", value: "—", icon: Users },
  { label: "Contributions this month", value: "—", icon: HandCoins },
  { label: "Collection rate", value: "—", icon: TrendingUp },
  { label: "Upcoming meetings", value: "—", icon: CalendarDays },
]

export function DashboardPage() {
  return (
    <PageLayout
      title="Dashboard"
      description="A clear view of your club's membership, finances, and upcoming activity."
    >
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {summaryCards.map((card) => {
          const Icon = card.icon

          return (
            <Card key={card.label} className="shadow-none">
              <CardHeader className="grid grid-cols-[1fr_auto] items-center gap-3">
                <CardTitle className="text-sm text-muted-foreground">{card.label}</CardTitle>
                <span className="grid size-9 place-items-center rounded-xl bg-accent text-accent-foreground">
                  <Icon className="size-[1.125rem]" aria-hidden="true" />
                </span>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-semibold tracking-tight">{card.value}</p>
                <p className="mt-2 text-xs text-muted-foreground">Data will appear when setup is complete.</p>
              </CardContent>
            </Card>
          )
        })}
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.4fr_1fr]">
        <Card className="min-h-72 shadow-none">
          <CardHeader>
            <CardTitle>Contribution overview</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 items-center justify-center text-center text-sm text-muted-foreground">
            Contribution trends will appear here once schedules and payments are available.
          </CardContent>
        </Card>
        <Card className="min-h-72 shadow-none">
          <CardHeader>
            <CardTitle>Upcoming activity</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 items-center justify-center text-center text-sm text-muted-foreground">
            Meetings, votes, and reminders will appear here.
          </CardContent>
        </Card>
      </div>
    </PageLayout>
  )
}
