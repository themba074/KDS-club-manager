import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { useRespond,useRsvp,type RsvpResponse } from "./participation-hooks"
export function RsvpControl({meetingId,canWrite,past}:{meetingId:string;canWrite:boolean;past:boolean}){const query=useRsvp(meetingId);const respond=useRespond(meetingId);const options:[RsvpResponse,string][]=[["YES","Yes"],["NO","No"],["MAYBE","Maybe"]];return <section className="space-y-2" aria-label="RSVP">
  <h4 className="font-medium">Your RSVP</h4>{query.isPending&&<p role="status">Loading RSVP…</p>}{query.error&&<p role="alert">{errorMessage(query.error)}</p>}
  {query.data&&<div className="flex flex-wrap gap-2">{options.map(([value,label])=><Button type="button" key={value} variant={query.data.response===value?"default":"outline"} disabled={past||respond.isPending} onClick={()=>respond.mutate(value)}>{label}</Button>)}</div>}
  {past&&<p className="text-sm text-muted-foreground">RSVPs are closed.</p>}{respond.error&&<p role="alert">{errorMessage(respond.error)}</p>}
  {canWrite&&query.data?.counts&&<p className="text-sm">Responses: {query.data.counts.yes} yes · {query.data.counts.no} no · {query.data.counts.maybe} maybe</p>}
  </section>}
