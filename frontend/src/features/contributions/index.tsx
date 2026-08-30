import { useState } from "react"
import { usePermission } from "@/features/roles/use-permission"
import { ScheduleForm } from "./ScheduleForm"
import { ScheduleList } from "./ScheduleList"
import type { ContributionSchedule } from "./schedule-hooks"

export function ContributionsPage() {
  const canWrite=usePermission("CONTRIBUTIONS_WRITE")
  const [editing,setEditing]=useState<ContributionSchedule|null>(null)
  return <section className="space-y-6">
    <header><h1 className="text-2xl font-semibold">Contributions</h1>
      <p className="mt-2 text-muted-foreground">Define expected contributions without moving or collecting money.</p></header>
    {canWrite&&<ScheduleForm editing={editing} onSaved={()=>setEditing(null)} onCancel={()=>setEditing(null)}/>}
    <ScheduleList canWrite={canWrite} onEdit={setEditing}/>
  </section>
}
