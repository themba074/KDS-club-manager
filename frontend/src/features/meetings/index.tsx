import { useState } from "react"
import { usePermission } from "@/features/roles/use-permission"
import { MeetingScheduler } from "./MeetingScheduler"
import { MeetingList } from "./MeetingList"
import type { Meeting } from "./meeting-hooks"

export function MeetingsPage() {
  const canWrite=usePermission("MEETINGS_WRITE")
  const [editing,setEditing]=useState<Meeting|null>(null)
  return <section className="space-y-6"><header><h1 className="text-2xl font-semibold">Meetings</h1><p className="mt-2 text-muted-foreground">Schedule club meetings and keep agendas in a clear order.</p></header>
    {canWrite&&<MeetingScheduler editing={editing} onSaved={()=>setEditing(null)} onCancel={()=>setEditing(null)}/>}<MeetingList canWrite={canWrite} onEdit={setEditing}/>
  </section>
}
