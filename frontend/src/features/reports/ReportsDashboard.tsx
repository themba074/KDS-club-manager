import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { errorMessage } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"
import { ReportExport } from "@/features/contributions/ReportExport"
import { useReportDownload, type ReportKind, type ReportFormat } from "./report-hooks"

const exports: { kind: ReportKind; title: string; description: string; permission: string; dated: boolean }[] = [
  { kind: "members", title: "Member list", description: "Current members and pending invitations.", permission: "MEMBERS_READ", dated: false },
  { kind: "meetings", title: "Meeting history", description: "Past meetings in the selected date range.", permission: "MEETINGS_READ", dated: true },
  { kind: "voting", title: "Voting history", description: "Closed and cancelled motions in the selected date range; private vote counts are excluded.", permission: "VOTES_READ", dated: true },
]

export function ReportsDashboard() {
  const year = new Date().getFullYear()
  const [from, setFrom] = useState(`${year}-01-01`)
  const [to, setTo] = useState(`${year}-12-31`)
  const permissions = useAuthStore(state => state.activeClub?.permissions ?? [])
  const download = useReportDownload()
  const available = exports.filter(item => permissions.includes(item.permission))
  const validRange = Boolean(from && to && from <= to)
  const exportReport = (kind: ReportKind, format: ReportFormat) => download.mutate({ kind, from, to, format })

  return <div className="space-y-8">
    <section className="space-y-5">
      <header><h1 className="text-2xl font-semibold">Reports dashboard</h1><p className="mt-2 text-muted-foreground">Export club records for administration, meetings, and audits.</p></header>
      <div className="flex flex-wrap items-end gap-3">
        <label>From<input aria-label="History from" className="block rounded-lg border bg-background p-2" type="date" value={from} onChange={event => setFrom(event.target.value)} /></label>
        <label>To<input aria-label="History to" className="block rounded-lg border bg-background p-2" type="date" value={to} onChange={event => setTo(event.target.value)} /></label>
      </div>
      {!validRange && <p role="alert">Choose a valid date range.</p>}
      {download.error && <p role="alert">{errorMessage(download.error)}</p>}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {available.map(item => <Card key={item.kind}>
          <CardHeader><CardTitle>{item.title}</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">{item.description}</p>
            {!item.dated && <p className="text-xs text-muted-foreground">The member list is a current snapshot; dates do not filter it.</p>}
            <div className="flex gap-2">
              <Button type="button" variant="outline" disabled={!validRange || download.isPending} onClick={() => exportReport(item.kind, "CSV")}>Export CSV</Button>
              <Button type="button" variant="outline" disabled={!validRange || download.isPending} onClick={() => exportReport(item.kind, "PDF")}>Export PDF</Button>
            </div>
          </CardContent>
        </Card>)}
      </div>
      {available.length === 0 && <p>No additional reports are available for your permissions.</p>}
    </section>
    <ReportExport />
  </div>
}
