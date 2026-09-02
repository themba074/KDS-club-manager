import { useState } from "react"
import { Card,CardContent,CardHeader,CardTitle } from "@/components/ui/card"
import { Table,TableBody,TableCell,TableHead,TableHeader,TableRow } from "@/components/ui/table"
import { errorMessage } from "@/features/auth/auth-api"
import { useMyLedger } from "./payment-hooks"

function initialRange(){const year=new Date().getFullYear();return [`${year}-01-01`,`${year}-12-31`] as const}
function money(value:number){return `R ${Number(value).toFixed(2)}`}
export function MyLedger(){
  const [initialFrom,initialTo]=initialRange(),[from,setFrom]=useState<string>(initialFrom),[to,setTo]=useState<string>(initialTo),ledger=useMyLedger(from,to)
  return <section className="space-y-4">
    <div className="flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-xl font-semibold">My ledger</h2><p className="text-sm text-muted-foreground">Only your authenticated membership can be shown here.</p></div>
      <div className="flex gap-3"><label>From<input className="block rounded-lg border bg-background p-2" type="date" value={from} onChange={event=>setFrom(event.target.value)}/></label><label>To<input className="block rounded-lg border bg-background p-2" type="date" value={to} onChange={event=>setTo(event.target.value)}/></label></div></div>
    {ledger.isPending&&<p role="status">Loading your ledger…</p>}{ledger.error&&<p role="alert">{errorMessage(ledger.error)}</p>}
    {ledger.data&&<><div className="grid gap-3 sm:grid-cols-3">
      <Card><CardHeader><CardTitle>Expected</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{money(ledger.data.totalExpected)}</CardContent></Card>
      <Card><CardHeader><CardTitle>Paid</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{money(ledger.data.totalPaid)}</CardContent></Card>
      <Card><CardHeader><CardTitle>Balance</CardTitle></CardHeader><CardContent className="text-2xl font-semibold">{money(ledger.data.balance)}</CardContent></Card>
    </div>
    {ledger.data.lines.length===0?<p>No ledger activity in this date range.</p>:<Table><TableHeader><TableRow><TableHead>Date</TableHead><TableHead>Activity</TableHead><TableHead>Expected</TableHead><TableHead>Paid</TableHead><TableHead>Running balance</TableHead></TableRow></TableHeader><TableBody>{ledger.data.lines.map((line,index)=><TableRow key={`${line.type}-${line.activityDate}-${line.paymentId??line.scheduleVersionId}-${index}`}><TableCell>{line.activityDate}</TableCell><TableCell>{line.description}</TableCell><TableCell>{line.expected?money(line.expected):"—"}</TableCell><TableCell>{line.paid?money(line.paid):"—"}</TableCell><TableCell>{money(line.runningBalance)}</TableCell></TableRow>)}</TableBody></Table>}</>}
  </section>
}
