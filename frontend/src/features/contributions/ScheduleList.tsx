import { useState } from "react"
import { Button } from "@/components/ui/button"
import { EmptyState } from "@/components/states/EmptyState"
import { ErrorState } from "@/components/states/ErrorState"
import { errorMessage } from "@/features/auth/auth-api"
import { CalendarRange, HandCoins } from "lucide-react"
import { useContributionSchedules,useUpcomingContributions,type ContributionSchedule } from "./schedule-hooks"
function local(offsetMonths=0){const date=new Date();date.setMonth(date.getMonth()+offsetMonths);return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`}
export function ScheduleList({canWrite,onEdit}:{canWrite:boolean;onEdit:(schedule:ContributionSchedule)=>void}){
  const schedules=useContributionSchedules();const [from,setFrom]=useState(local()),[to,setTo]=useState(local(3));const upcoming=useUpcomingContributions(from,to)
  return <div className="space-y-6">
    <section className="space-y-3"><h2 className="text-xl font-semibold">Schedules</h2>
      {schedules.isPending&&<p role="status">Loading schedules…</p>}{schedules.error&&<ErrorState title="We couldn't load contribution schedules" description={errorMessage(schedules.error, "Try loading the schedules again.")} onRetry={() => void schedules.refetch()}/>}
      {schedules.data?.length===0&&<EmptyState icon={HandCoins} title="No contribution schedules yet" description={canWrite?"Create a schedule above to tell members what is due and when.":"A club manager has not created a contribution schedule yet."}/>}
      <ul className="grid gap-4 md:grid-cols-2">{schedules.data?.map(schedule=><li className="space-y-2 rounded-xl border bg-card p-4" key={schedule.scheduleId}>
        <h3 className="font-semibold">{schedule.name}</h3><p>R {Number(schedule.amount).toFixed(2)} · {schedule.frequency==="MONTHLY"?"monthly":"once-off"}</p>
        <p className="text-sm">Revision {schedule.versionNumber}, effective {schedule.effectiveFrom}</p><p className="text-sm">{schedule.assignedMembers.length} snapshotted members · {schedule.assignmentMode==="ALL_CURRENT"?"all active at creation":"selected"}</p>
        {canWrite&&<Button type="button" variant="outline" onClick={()=>onEdit(schedule)}>Create revision</Button>}
      </li>)}</ul>
    </section>
    <section className="space-y-3"><h2 className="text-xl font-semibold">Upcoming expected contributions</h2>
      <div className="flex flex-wrap gap-3"><label>From<input className="block rounded-lg border bg-background p-2" type="date" value={from} onChange={e=>setFrom(e.target.value)}/></label><label>To<input className="block rounded-lg border bg-background p-2" type="date" value={to} onChange={e=>setTo(e.target.value)}/></label></div>
      {upcoming.isPending&&<p role="status">Calculating expectations…</p>}{upcoming.error&&<ErrorState title="We couldn't calculate expected contributions" description={errorMessage(upcoming.error, "Check the date range and try again.")} onRetry={() => void upcoming.refetch()}/>}
      {upcoming.data?.length===0&&<EmptyState icon={CalendarRange} title="Nothing is due in this date range" description="Choose a wider date range, or create a contribution schedule if the club has not set one up."/>}
      <ul className="space-y-2">{upcoming.data?.map((item,index)=><li className="rounded-lg border p-3" key={`${item.scheduleVersionId}-${item.membershipId}-${item.dueDate}-${index}`}><strong>{item.dueDate}</strong> · {item.scheduleName} · {item.memberName} · R {Number(item.amount).toFixed(2)}</li>)}</ul>
    </section>
  </div>
}
