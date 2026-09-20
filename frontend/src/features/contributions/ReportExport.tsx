import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card,CardContent,CardHeader,CardTitle } from "@/components/ui/card"
import { Table,TableBody,TableCell,TableHead,TableHeader,TableRow } from "@/components/ui/table"
import { errorMessage } from "@/features/auth/auth-api"
import { EmptyState } from "@/components/states/EmptyState"
import { ErrorState } from "@/components/states/ErrorState"
import { ChartNoAxesCombined } from "lucide-react"
import { useContributionReport,useContributionReportExport,type ReportFormat } from "./report-hooks"

function initialRange(){const year=new Date().getFullYear();return [`${year}-01-01`,`${year}-12-31`] as const}
function money(value:number){return `R ${Number(value).toFixed(2)}`}

export function ReportExport(){
  const [initialFrom,initialTo]=initialRange(),[from,setFrom]=useState<string>(initialFrom),[to,setTo]=useState<string>(initialTo)
  const report=useContributionReport(from,to),download=useContributionReportExport()
  const exportReport=(format:ReportFormat)=>download.mutate({from,to,format})
  return <section className="space-y-5">
    <header><h1 className="text-2xl font-semibold">Contribution reports</h1><p className="mt-2 text-muted-foreground">Review club-wide collections and export the same figures for an AGM or audit.</p></header>
    <div className="flex flex-wrap items-end gap-3">
      <label>From<input aria-label="Report from" className="block rounded-lg border bg-background p-2" type="date" value={from} onChange={event=>setFrom(event.target.value)}/></label>
      <label>To<input aria-label="Report to" className="block rounded-lg border bg-background p-2" type="date" value={to} onChange={event=>setTo(event.target.value)}/></label>
      <Button type="button" variant="outline" disabled={download.isPending||!report.data} onClick={()=>exportReport("CSV")}>Export CSV</Button>
      <Button type="button" variant="outline" disabled={download.isPending||!report.data} onClick={()=>exportReport("PDF")}>Export PDF</Button>
    </div>
    {report.isPending&&<p role="status">Calculating contribution report…</p>}{report.error&&<ErrorState title="We couldn't calculate this contribution report" description={errorMessage(report.error, "Check the date range and try again.")} onRetry={() => void report.refetch()}/>} {download.error&&<p role="alert" className="text-destructive">{errorMessage(download.error, "We couldn't download the report. Try the export again.")}</p>}
    {report.data&&<>
      <div className="grid gap-3 sm:grid-cols-3">
        <Card><CardHeader><CardTitle>Total expected</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{money(report.data.totalExpected)}</CardContent></Card>
        <Card><CardHeader><CardTitle>Total collected</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{money(report.data.totalCollected)}</CardContent></Card>
        <Card><CardHeader><CardTitle>Total outstanding</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{money(report.data.totalOutstanding)}</CardContent></Card>
      </div>
      {report.data.members.length===0?<EmptyState icon={ChartNoAxesCombined} title="No contribution activity in this date range" description="Choose a wider date range, or return after schedules and payments have been recorded."/>:<Table><TableHeader><TableRow><TableHead>Member</TableHead><TableHead>Email</TableHead><TableHead>Expected</TableHead><TableHead>Collected</TableHead><TableHead>Outstanding</TableHead></TableRow></TableHeader><TableBody>{report.data.members.map(member=><TableRow key={member.membershipId}><TableCell>{member.memberName}</TableCell><TableCell>{member.memberEmail}</TableCell><TableCell>{money(member.expected)}</TableCell><TableCell>{money(member.collected)}</TableCell><TableCell>{money(member.outstanding)}</TableCell></TableRow>)}</TableBody></Table>}
    </>}
  </section>
}
