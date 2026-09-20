import { useState } from "react"
import { usePermission } from "@/features/roles/use-permission"
import { ScheduleForm } from "./ScheduleForm"
import { ScheduleList } from "./ScheduleList"
import type { ContributionSchedule } from "./schedule-hooks"
import { RecordPayment } from "./RecordPayment"
import { MyLedger } from "./MyLedger"

export function ContributionsPage() {
  const canWrite=usePermission("CONTRIBUTIONS_WRITE")
  const [editing,setEditing]=useState<ContributionSchedule|null>(null)
  return <section className="space-y-7">
    <header><p className="text-sm font-medium text-primary">Club finances</p><h1 className="mt-1 font-heading text-3xl font-semibold tracking-[-0.035em]">Contributions</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">Define expected contributions, record receipts, and understand your balance.</p></header>
    <MyLedger/>
    {canWrite&&<RecordPayment/>}
    {canWrite&&<ScheduleForm editing={editing} onSaved={()=>setEditing(null)} onCancel={()=>setEditing(null)}/>}
    <ScheduleList canWrite={canWrite} onEdit={setEditing}/>
  </section>
}
