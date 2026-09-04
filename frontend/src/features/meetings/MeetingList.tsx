import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { useMeetings,type Meeting } from "./meeting-hooks"
import { useState } from "react"
import { RsvpControl } from "./RsvpControl"
import { MinutesEditor } from "./MinutesEditor"
function Participation({meeting,canWrite,view}:{meeting:Meeting;canWrite:boolean;view:"UPCOMING"|"PAST"}){const [open,setOpen]=useState(false);return <div className="space-y-3"><Button type="button" variant="outline" onClick={()=>setOpen(value=>!value)}>{open?"Hide participation and minutes":"Show participation and minutes"}</Button>{open&&<><RsvpControl meetingId={meeting.id} canWrite={canWrite} past={view==="PAST"}/><MinutesEditor meetingId={meeting.id} canWrite={canWrite} past={view==="PAST"}/></>}</div>}
function Meetings({view,canWrite,onEdit}:{view:"UPCOMING"|"PAST";canWrite:boolean;onEdit:(meeting:Meeting)=>void}){const meetings=useMeetings(view);return <section className="space-y-3"><h2 className="text-xl font-semibold">{view==="UPCOMING"?"Upcoming meetings":"Past meetings"}</h2>
  {meetings.isPending&&<p role="status">Loading {view.toLowerCase()} meetings…</p>}{meetings.error&&<p role="alert">{errorMessage(meetings.error)}</p>}{meetings.data?.length===0&&<p>No {view.toLowerCase()} meetings.</p>}
  <ul className="space-y-4">{meetings.data?.map(meeting=><li className="space-y-2 rounded-xl border bg-card p-4" key={meeting.id}><h3 className="font-semibold">{meeting.title}</h3><p>{new Intl.DateTimeFormat(undefined,{dateStyle:"medium",timeStyle:"short"}).format(new Date(meeting.startsAt))} · {meeting.durationMinutes} minutes</p>
    <p>{meeting.location||"Online"}{meeting.meetingUrl&&<> · <a className="underline" href={meeting.meetingUrl} target="_blank" rel="noreferrer">Join online</a></>}</p>{meeting.description&&<p className="text-sm text-muted-foreground">{meeting.description}</p>}
    <h4 className="font-medium">Agenda</h4><ol className="list-decimal space-y-1 pl-5">{meeting.agendaItems.map(item=><li key={item.id}>{item.title}{item.description&&<span className="text-muted-foreground"> — {item.description}</span>}</li>)}</ol>
    {view==="UPCOMING"&&canWrite&&<Button type="button" variant="outline" onClick={()=>onEdit(meeting)}>Edit meeting</Button>}<Participation meeting={meeting} canWrite={canWrite} view={view}/></li>)}</ul></section>}
export function MeetingList(props:{canWrite:boolean;onEdit:(meeting:Meeting)=>void}){return <div className="space-y-8"><Meetings view="UPCOMING" {...props}/><Meetings view="PAST" {...props}/></div>}
