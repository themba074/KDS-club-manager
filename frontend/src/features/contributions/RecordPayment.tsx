import { useEffect,useMemo,useState } from "react"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useRecordableExpectations,useRecordPayment,type PaymentInput } from "./payment-hooks"

function dateValue(date=new Date()){return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`}
function yearRange(){const year=new Date().getFullYear();return [`${year}-01-01`,`${year}-12-31`] as const}
function key(item:{scheduleVersionId:string;membershipId:string;dueDate:string}){return `${item.scheduleVersionId}|${item.membershipId}|${item.dueDate}`}

export function RecordPayment(){
  const [from,to]=yearRange(),expectations=useRecordableExpectations(from,to,true),save=useRecordPayment()
  const outstanding=useMemo(()=>expectations.data?.filter(item=>Number(item.outstanding)>0)??[],[expectations.data])
  const [selected,setSelected]=useState("");const {register,handleSubmit,reset,setValue,formState:{errors}}=useForm<PaymentInput>({defaultValues:{amount:"",receivedOn:dateValue(),reference:"",note:""}})
  const selectedKey=selected||(outstanding[0]?key(outstanding[0]):"")
  const expectation=outstanding.find(item=>key(item)===selectedKey)
  useEffect(()=>{if(expectation)setValue("amount",Number(expectation.outstanding).toFixed(2))},[expectation,setValue])
  return <section className="space-y-3 rounded-xl border bg-card p-4">
    <div><h2 className="text-xl font-semibold">Record a payment</h2><p className="text-sm text-muted-foreground">Allocate a received payment to its exact member contribution.</p></div>
    {expectations.isPending&&<p role="status">Loading outstanding contributions…</p>}{expectations.error&&<p role="alert">{errorMessage(expectations.error)}</p>}
    {!expectations.isPending&&outstanding.length===0&&<p>No outstanding contributions are available in this calendar year.</p>}
    {outstanding.length>0&&<form className="space-y-4" onSubmit={handleSubmit(input=>expectation&&save.mutate({input:{...input,scheduleVersionId:expectation.scheduleVersionId,membershipId:expectation.membershipId,dueDate:expectation.dueDate},proof:(document.getElementById("payment-proof") as HTMLInputElement)?.files?.[0]},{onSuccess:()=>{reset({amount:Number(expectation.outstanding).toFixed(2),receivedOn:dateValue(),reference:"",note:""});const proof=document.getElementById("payment-proof") as HTMLInputElement;if(proof)proof.value=""}}))}>
      <label>Contribution<select className="block w-full rounded-lg border bg-background p-2" value={selectedKey} onChange={event=>setSelected(event.target.value)}>{outstanding.map(item=><option key={key(item)} value={key(item)}>{item.memberName} · {item.scheduleName} · due {item.dueDate} · R {Number(item.outstanding).toFixed(2)} outstanding</option>)}</select></label>
      <div className="grid gap-4 md:grid-cols-2">
        <label>Amount (ZAR)<Input type="number" min="0.01" step="0.01" {...register("amount",{required:"Amount is required",min:{value:0.01,message:"Amount must be positive"}})}/>{errors.amount&&<span role="alert">{errors.amount.message}</span>}</label>
        <label>Received on<Input type="date" max={dateValue()} {...register("receivedOn",{required:"Payment date is required"})}/></label>
        <label>Reference (optional)<Input maxLength={120} {...register("reference")}/></label>
        <label>Proof (optional)<Input id="payment-proof" type="file" accept="application/pdf,image/jpeg,image/png"/></label>
      </div>
      <label>Note (optional)<textarea className="block min-h-20 w-full rounded-lg border bg-background p-2" maxLength={500} {...register("note")}/></label>
      {save.error&&<p role="alert" className="text-destructive">{errorMessage(save.error)}</p>}
      <Button type="submit" disabled={save.isPending}>{save.isPending?"Recording…":"Mark payment received"}</Button>
    </form>}
  </section>
}
