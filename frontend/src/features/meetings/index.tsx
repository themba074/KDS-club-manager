import { useState } from "react"
import { CalendarDays, Clock3, UsersRound } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { usePermission } from "@/features/roles/use-permission"
import { MeetingScheduler } from "./MeetingScheduler"
import { MeetingList } from "./MeetingList"
import { useMeetings, type Meeting } from "./meeting-hooks"
import { useRsvp } from "./participation-hooks"

function NextMeetingOverview({ meeting, canWrite }: { meeting: Meeting; canWrite: boolean }) {
  const rsvp = useRsvp(meeting.id, canWrite)
  const startsAt = new Date(meeting.startsAt)
  const counts = rsvp.data?.counts
  return (
    <div className="grid gap-4 xl:grid-cols-[1.45fr_1fr]">
      <Card className="border-primary/15 bg-[linear-gradient(115deg,oklch(0.985_0.012_155),oklch(0.945_0.04_155))] shadow-none">
        <CardHeader className="flex-row items-center justify-between gap-3">
          <div><p className="text-sm font-medium text-primary">Next meeting</p><CardTitle className="mt-1 text-xl">Next: {meeting.title}</CardTitle></div>
          <span className="grid size-10 place-items-center rounded-xl bg-primary/10 text-primary"><CalendarDays className="size-5" aria-hidden="true" /></span>
        </CardHeader>
        <CardContent className="grid gap-3 text-sm text-muted-foreground sm:grid-cols-2">
          <span className="inline-flex items-center gap-2"><Clock3 className="size-4 text-primary" aria-hidden="true" />{new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(startsAt)}</span>
          <span>{meeting.location || "Online meeting"} · {meeting.durationMinutes} minutes</span>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex-row items-center justify-between gap-3"><div><p className="text-sm font-medium text-primary">RSVP overview</p><CardTitle className="mt-1 text-base">Responses for this meeting</CardTitle></div><UsersRound className="size-5 text-primary" aria-hidden="true" /></CardHeader>
        <CardContent>
          {!canWrite ? <p className="text-sm leading-6 text-muted-foreground">RSVP totals are available to meeting managers. You can still record your response in the meeting details below.</p> : rsvp.isPending ? <p className="text-sm text-muted-foreground">Loading RSVP responses…</p> : rsvp.error ? <div role="alert" className="space-y-2 text-sm"><p>{errorMessage(rsvp.error, "We couldn't load RSVP totals.")}</p><Button type="button" variant="outline" size="sm" onClick={() => void rsvp.refetch()}>Try again</Button></div> : counts ? <div className="grid grid-cols-3 gap-2 text-center text-sm"><div className="rounded-lg bg-primary/10 p-2"><strong className="block text-base text-primary">{counts.yes}</strong><span className="text-muted-foreground">Going</span></div><div className="rounded-lg bg-amber-500/10 p-2"><strong className="block text-base text-amber-700">{counts.maybe}</strong><span className="text-muted-foreground">Maybe</span></div><div className="rounded-lg bg-destructive/10 p-2"><strong className="block text-base text-destructive">{counts.no}</strong><span className="text-muted-foreground">Declined</span></div></div> : <p className="text-sm text-muted-foreground">No RSVP responses yet.</p>}
        </CardContent>
      </Card>
    </div>
  )
}

export function MeetingsPage() {
  const canWrite=usePermission("MEETINGS_WRITE")
  const [editing,setEditing]=useState<Meeting|null>(null)
  const upcoming = useMeetings("UPCOMING")
  return <section className="space-y-7"><header><p className="text-sm font-medium text-primary">Club governance</p><h1 className="mt-1 font-heading text-3xl font-semibold tracking-[-0.035em]">Meetings</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">Schedule club meetings, keep agendas in a clear order, and track participation.</p></header>
    {upcoming.data?.[0] && <NextMeetingOverview meeting={upcoming.data[0]} canWrite={canWrite} />}
    {canWrite&&<MeetingScheduler editing={editing} onSaved={()=>setEditing(null)} onCancel={()=>setEditing(null)}/>}<MeetingList canWrite={canWrite} onEdit={setEditing}/>
  </section>
}
