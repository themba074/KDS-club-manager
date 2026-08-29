import { useState } from "react"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { useContributionSchedules,useUpcomingContributions,type ContributionSchedule } from "./schedule-hooks"
function local(offsetMonths=0){const date=new Date();date.setMonth(date.getMonth()+offsetMonths);return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`}
export function ScheduleList({canWrite,onEdit}:{canWrite:boolean;onEdit:(schedule:ContributionSchedule)=>void}){
  const schedules=useContributionSchedules();const [from,setFrom]=useState(local()),[to,setTo]=useState(local(3));const upcoming=useUpcomingContributions(from,to)
  return <div className="space-y-6">
    <section className="space-y-3"><h2 className="text-xl font-semibold">Schedules</h2>
      {schedules.isPending&&<p role="status">Loading schedules…</p>}{schedules.error&&<p role="alert">{errorMessage(schedules.error)}</p>}
      {schedules.data?.length===0&&<p>No contribution schedules yet.</p>}
      <ul className="grid gap-4 md:grid-cols-2">{schedules.data?.map(schedule=><li className="space-y-2 rounded-xl border bg-card p-4" key={schedule.scheduleId}>
        <h3 className="font-semibold">{schedule.name}</h3><p>R {Number(schedule.amount).toFixed(2)} · {schedule.frequency==="MONTHLY"?"monthly":"once-off"}</p>
        <p className="text-sm">Revision {schedule.versionNumber}, effective {schedule.effectiveFrom}</p><p className="text-sm">{schedule.assignedMembers.length} snapshotted members · {schedule.assignmentMode==="ALL_CURRENT"?"all active at creation":"selected"}</p>
        {canWrite&&<Button type="button" variant="outline" onClick={()=>onEdit(schedule)}>Create revision</Button>}
      </li>)}</ul>
    </section>
    <section className="space-y-3"><h2 className="text-xl font-semibold">Upcoming expected contributions</h2>
      <div className="flex flex-wrap gap-3"><label>From<input className="block rounded-lg border bg-background p-2" type="date" value={from} onChange={e=>setFrom(e.target.value)}/></label><label>To<input className="block rounded-lg border bg-background p-2" type="date" value={to} onChange={e=>setTo(e.target.value)}/></label></div>
      {upcoming.isPending&&<p role="status">Calculating expectations…</p>}{upcoming.error&&<p role="alert">{errorMessage(upcoming.error)}</p>}
      {upcoming.data?.length===0&&<p>No expected contributions in this date range.</p>}
      <ul className="space-y-2">{upcoming.data?.map((item,index)=><li className="rounded-lg border p-3" key={`${item.scheduleVersionId}-${item.membershipId}-${item.dueDate}-${index}`}><strong>{item.dueDate}</strong> · {item.scheduleName} · {item.memberName} · R {Number(item.amount).toFixed(2)}</li>)}</ul>
    </section>
  </div>
}
