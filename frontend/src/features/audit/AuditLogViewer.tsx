import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { EmptyState } from "@/components/states/EmptyState"
import { ErrorState } from "@/components/states/ErrorState"
import { ListFilter } from "lucide-react"
import { auditActions,useAuditLog,type AuditAction,type AuditFilters } from "./audit-hooks"

const labels:Record<AuditAction,string>={PAYMENT_RECORDED:"Payment recorded",VOTE_CAST:"Vote cast",VOTE_RESULTS_PUBLISHED:"Results published",ROLE_ASSIGNED:"Role assigned",DOCUMENT_UPLOADED:"Document uploaded",DOCUMENT_METADATA_UPDATED:"Document updated",DOCUMENT_VERSION_UPLOADED:"New document version",MEMBER_STATUS_CHANGED:"Member status changed"}
const initial:AuditFilters={actor:"",action:"",from:"",to:"",page:0}

export function AuditLogViewer(){
  const [filters,setFilters]=useState<AuditFilters>(initial)
  const log=useAuditLog(filters)
  const change=(patch:Partial<AuditFilters>)=>setFilters(previous=>({...previous,...patch,page:patch.page??0}))
  const validActor=!filters.actor.trim()||/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(filters.actor.trim())
  return <section className="space-y-5">
    <header><h1 className="text-2xl font-semibold">Audit log</h1><p className="mt-2 text-muted-foreground">An append-only record of important actions in this club.</p></header>
    <div className="grid gap-3 rounded-xl border bg-card p-4 sm:grid-cols-2 lg:grid-cols-4">
      <label>Actor ID<Input aria-label="Actor ID" placeholder="User UUID" value={filters.actor} onChange={event=>change({actor:event.target.value})}/></label>
      <label>Action<select aria-label="Action" className="block h-9 w-full rounded-md border bg-background px-3" value={filters.action} onChange={event=>change({action:event.target.value as AuditAction|""})}><option value="">All actions</option>{auditActions.map(action=><option key={action} value={action}>{labels[action]}</option>)}</select></label>
      <label>From<Input aria-label="From date" type="date" value={filters.from} onChange={event=>change({from:event.target.value})}/></label>
      <label>To<Input aria-label="To date" type="date" value={filters.to} onChange={event=>change({to:event.target.value})}/></label>
    </div>
    {!validActor&&<p role="alert" className="text-destructive">Enter a valid actor UUID.</p>}
    {log.isPending&&validActor&&<p role="status">Loading audit entries…</p>}
    {log.error && <ErrorState title="We couldn't load the audit log" description={errorMessage(log.error, "Check the filters and try loading the audit log again.")} onRetry={() => void log.refetch()}/>}
    {log.data?.content.length === 0 && <EmptyState icon={ListFilter} title="No audit entries match these filters" description="Clear one or more filters, or return after members have started using the club workspace." action={<Button variant="outline" onClick={() => setFilters(initial)}>Clear filters</Button>}/>}
    <div className="space-y-3">{log.data?.content.map(entry=><article key={entry.id} className="rounded-xl border bg-card p-4"><div className="flex flex-wrap justify-between gap-2"><h2 className="font-semibold">{labels[entry.action]}</h2><time className="text-sm text-muted-foreground" dateTime={entry.occurredAt}>{new Date(entry.occurredAt).toLocaleString()}</time></div><p className="mt-1 break-all text-sm">Actor: {entry.actorId}</p><p className="break-all text-sm">{entry.entityType}: {entry.entityId}</p>{(entry.previousValue||entry.newValue)&&<p className="text-sm text-muted-foreground">{entry.previousValue??"—"} → {entry.newValue??"—"}</p>}</article>)}</div>
    {log.data&&<div className="flex items-center justify-between gap-3"><p className="text-sm text-muted-foreground">{log.data.totalElements} entries · page {log.data.page+1} of {Math.max(log.data.totalPages,1)}</p><div className="flex gap-2"><Button variant="outline" disabled={filters.page===0} onClick={()=>change({page:filters.page-1})}>Previous</Button><Button variant="outline" disabled={filters.page+1>=log.data.totalPages} onClick={()=>change({page:filters.page+1})}>Next</Button></div></div>}
  </section>
}
