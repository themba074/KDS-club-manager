import { useEffect } from "react"
import { useForm,useWatch } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useAssignableMembers,useSaveSchedule,type ContributionSchedule,type ScheduleInput } from "./schedule-hooks"
function localDate(offset=0){const date=new Date();date.setDate(date.getDate()+offset);return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`}
function dayAfter(value:string){const [year,month,day]=value.split("-").map(Number);const date=new Date(year,month-1,day+1);return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`}
const defaults:ScheduleInput={name:"",amount:"",frequency:"MONTHLY",firstDueDate:localDate(1),endDate:null,effectiveFrom:localDate(),assignmentMode:"ALL_CURRENT",membershipIds:[]}
export function ScheduleForm({editing,onSaved,onCancel}:{editing:ContributionSchedule|null;onSaved:()=>void;onCancel:()=>void}){
  const members=useAssignableMembers(true),save=useSaveSchedule()
  const {register,handleSubmit,control,reset,formState:{errors}}=useForm<ScheduleInput>({defaultValues:defaults})
  const effectiveMinimum=editing&&dayAfter(editing.effectiveFrom)>localDate()?dayAfter(editing.effectiveFrom):localDate()
  useEffect(()=>{reset(editing?{name:editing.name,amount:String(editing.amount),frequency:editing.frequency,firstDueDate:editing.firstDueDate,endDate:editing.endDate,effectiveFrom:effectiveMinimum,assignmentMode:editing.assignmentMode,membershipIds:editing.assignedMembers.map(m=>m.membershipId)}:defaults)},[editing,effectiveMinimum,reset])
  const frequency=useWatch({control,name:"frequency"}),mode=useWatch({control,name:"assignmentMode"})
  return <form className="space-y-4 rounded-xl border bg-card p-4" onSubmit={handleSubmit(input=>save.mutate({scheduleId:editing?.scheduleId,input:{...input,endDate:frequency==="ONCE_OFF"?null:input.endDate||null,membershipIds:mode==="ALL_CURRENT"?[]:input.membershipIds}},{onSuccess:()=>{reset(defaults);onSaved()}}))}>
    <h2 className="text-lg font-semibold">{editing?`Create revision ${editing.versionNumber+1}`:"Create schedule"}</h2>
    {editing&&<p className="text-sm text-muted-foreground">The current revision remains unchanged through the day before the new effective date.</p>}
    <div className="grid gap-4 md:grid-cols-2">
      <label>Name<Input maxLength={120} {...register("name",{required:"Name is required"})}/>{errors.name&&<span role="alert">{errors.name.message}</span>}</label>
      <label>Amount (ZAR)<Input type="number" min="0.01" step="0.01" {...register("amount",{required:"Amount is required"})}/></label>
      <label>Frequency<select className="block w-full rounded-lg border bg-background p-2" {...register("frequency")}><option value="MONTHLY">Monthly</option><option value="ONCE_OFF">Once-off</option></select></label>
      <label>First due date<Input type="date" {...register("firstDueDate",{required:true})}/></label>
      {frequency==="MONTHLY"&&<label>End date (optional)<Input type="date" {...register("endDate")}/></label>}
      <label>Effective from<Input type="date" min={effectiveMinimum} {...register("effectiveFrom",{required:true})}/></label>
      <label>Assign to<select className="block w-full rounded-lg border bg-background p-2" {...register("assignmentMode")}><option value="ALL_CURRENT">All currently active members</option><option value="SELECTED">Selected active members</option></select></label>
    </div>
    {mode==="SELECTED"&&<fieldset className="space-y-2"><legend className="font-medium">Members</legend>
      {members.isPending&&<p role="status">Loading active members…</p>}{members.data?.map(member=><label className="flex gap-2" key={member.membershipId}><input type="checkbox" value={member.membershipId} {...register("membershipIds")}/><span>{member.displayName} ({member.email})</span></label>)}
      {members.data?.length===0&&<p>No active members are available.</p>}
    </fieldset>}
    {save.error&&<p role="alert" className="text-destructive">{errorMessage(save.error)}</p>}
    <div className="flex gap-2"><Button type="submit" disabled={save.isPending}>{save.isPending?"Saving…":editing?"Save new revision":"Create schedule"}</Button>{editing&&<Button type="button" variant="outline" onClick={onCancel}>Cancel</Button>}</div>
  </form>
}
