import { useRef } from "react"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { api } from "@/features/auth/auth-api"
import { useAttachMinutes,useMinutes,usePublishMinutes,useSaveMinutes } from "./participation-hooks"
function status(error:unknown){return typeof error==="object"&&error!==null&&"response" in error?(error as {response?:{status?:number}}).response?.status:undefined}
export function MinutesEditor({meetingId,canWrite,past}:{meetingId:string;canWrite:boolean;past:boolean}){const query=useMinutes(meetingId);const save=useSaveMinutes(meetingId);const attach=useAttachMinutes(meetingId);const publish=usePublishMinutes(meetingId);const body=useRef<HTMLTextAreaElement>(null);
  const missing=status(query.error)===404||status(query.error)===403;const version=query.data?.version??0;
  const download=async()=>{const response=await api.get(`/meetings/${meetingId}/minutes/attachment`,{responseType:"blob"});const url=URL.createObjectURL(response.data);const anchor=document.createElement("a");anchor.href=url;anchor.download=query.data?.attachmentName??"meeting-minutes";anchor.click();URL.revokeObjectURL(url)}
  return <section className="space-y-2" aria-label="Meeting minutes"><h4 className="font-medium">Minutes</h4>{query.isPending&&<p role="status">Loading minutes…</p>}
    {query.error&&!missing&&<p role="alert">{errorMessage(query.error)}</p>}{missing&&!canWrite&&<p>Minutes have not been published.</p>}
    {query.data?.publishedAt&&<p className="text-sm text-muted-foreground">Published {new Intl.DateTimeFormat(undefined,{dateStyle:"medium",timeStyle:"short"}).format(new Date(query.data.publishedAt))}</p>}
    {canWrite&&past&&<><label className="grid gap-1">Meeting notes<textarea key={`${query.data?.id??"new"}-${version}`} ref={body} className="min-h-40 rounded-md border bg-background p-3" maxLength={20000} defaultValue={query.data?.body??""} placeholder="Record decisions, actions, and discussion notes…" /></label><p className="text-xs text-muted-foreground">Use clear headings and lists; text is displayed safely without executing HTML.</p><Button type="button" disabled={save.isPending} onClick={()=>save.mutate({version,body:body.current?.value??""})}>Save draft</Button>
      <label className="block text-sm">Attach PDF, DOCX, or text file (maximum 5 MB)<input className="mt-1 block" type="file" accept=".pdf,.docx,.txt" onChange={event=>{const file=event.target.files?.[0];if(file)attach.mutate({version:query.data?.version??0,file})}} /></label>
      {query.data&&!query.data.published&&<Button type="button" variant="outline" disabled={publish.isPending} onClick={()=>publish.mutate(query.data.version)}>Publish minutes</Button>}</>}
    {query.data?.body&&(!canWrite||!past)&&<p className="whitespace-pre-wrap rounded-md bg-muted p-3">{query.data.body}</p>}
    {query.data?.attachmentName&&<Button type="button" variant="outline" onClick={()=>void download()}>Download {query.data.attachmentName}</Button>}
    {(save.error||attach.error||publish.error)&&<p role="alert">{errorMessage(save.error||attach.error||publish.error)}</p>}</section>}
