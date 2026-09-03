import { useEffect } from "react"
import { useFieldArray,useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useSaveMeeting,type Meeting } from "./meeting-hooks"
type Form={title:string;description:string;startsAt:string;durationMinutes:number;location:string;meetingUrl:string;agendaItems:{title:string;description:string}[]}
function localDateTime(date:Date){const local=new Date(date);local.setMinutes(local.getMinutes()-local.getTimezoneOffset());return local.toISOString().slice(0,16)}
function futureLocal(){return localDateTime(new Date(Date.now()+86400000))}
function minimumLocal(){return localDateTime(new Date(Date.now()+60000))}
function fromOffsetDateTime(value:string){return localDateTime(new Date(value))}
function offsetDateTime(value:string){const date=new Date(value);const minutes=-date.getTimezoneOffset();const sign=minutes>=0?"+":"-";const absolute=Math.abs(minutes);return `${value}:00${sign}${String(Math.floor(absolute/60)).padStart(2,"0")}:${String(absolute%60).padStart(2,"0")}`}
const defaults:Form={title:"",description:"",startsAt:futureLocal(),durationMinutes:60,location:"",meetingUrl:"",agendaItems:[{title:"",description:""}]}
export function MeetingScheduler({editing,onSaved,onCancel}:{editing:Meeting|null;onSaved:()=>void;onCancel:()=>void}){
  const save=useSaveMeeting();const {register,control,handleSubmit,reset,formState:{errors}}=useForm<Form>({defaultValues:defaults});const agenda=useFieldArray({control,name:"agendaItems"})
  useEffect(()=>reset(editing?{title:editing.title,description:editing.description??"",startsAt:fromOffsetDateTime(editing.startsAt),durationMinutes:editing.durationMinutes,location:editing.location??"",meetingUrl:editing.meetingUrl??"",agendaItems:editing.agendaItems.map(item=>({title:item.title,description:item.description??""}))}:defaults),[editing,reset])
  return <form className="space-y-4 rounded-xl border bg-card p-4" onSubmit={handleSubmit(values=>save.mutate({meetingId:editing?.id,input:{...values,version:editing?.version??0,startsAt:offsetDateTime(values.startsAt)}},{onSuccess:()=>{reset(defaults);onSaved()}}))}>
    <h2 className="text-lg font-semibold">{editing?"Edit meeting":"Schedule a meeting"}</h2>{editing&&<p className="text-sm text-muted-foreground">Saving an edit creates a notification event for active members.</p>}
    <div className="grid gap-4 md:grid-cols-2"><label>Title<Input maxLength={160} {...register("title",{required:"Title is required"})}/>{errors.title&&<span role="alert">{errors.title.message}</span>}</label>
      <label>Starts at<Input type="datetime-local" min={minimumLocal()} {...register("startsAt",{required:true})}/></label><label>Duration (minutes)<Input type="number" min={15} max={1440} {...register("durationMinutes",{valueAsNumber:true,required:true})}/></label>
      <label>Physical location<Input maxLength={240} {...register("location")}/></label><label>Online meeting link<Input type="url" maxLength={500} placeholder="https://…" {...register("meetingUrl")}/></label></div>
    <label className="block">Description<textarea className="mt-1 min-h-20 w-full rounded-lg border bg-background p-2" maxLength={4000} {...register("description")}/></label>
    <fieldset className="space-y-3"><legend className="text-lg font-semibold">Agenda</legend>{agenda.fields.map((field,index)=><div className="space-y-2 rounded-lg border p-3" key={field.id}>
      <label>Item {index+1} title<Input maxLength={200} {...register(`agendaItems.${index}.title`,{required:"Agenda title is required"})}/></label><label className="block">Description<textarea className="mt-1 min-h-16 w-full rounded-lg border bg-background p-2" maxLength={2000} {...register(`agendaItems.${index}.description`)}/></label>
      <div className="flex flex-wrap gap-2"><Button type="button" variant="outline" disabled={index===0} onClick={()=>agenda.move(index,index-1)}>Move up</Button><Button type="button" variant="outline" disabled={index===agenda.fields.length-1} onClick={()=>agenda.move(index,index+1)}>Move down</Button><Button type="button" variant="outline" disabled={agenda.fields.length===1} onClick={()=>agenda.remove(index)}>Remove</Button></div>
    </div>)}<Button type="button" variant="outline" disabled={agenda.fields.length>=50} onClick={()=>agenda.append({title:"",description:""})}>Add agenda item</Button></fieldset>
    {save.error&&<p role="alert" className="text-destructive">{errorMessage(save.error)}</p>}<div className="flex gap-2"><Button type="submit" disabled={save.isPending}>{save.isPending?"Saving…":editing?"Save meeting":"Schedule meeting"}</Button>{editing&&<Button type="button" variant="outline" onClick={onCancel}>Cancel</Button>}</div>
  </form>
}
