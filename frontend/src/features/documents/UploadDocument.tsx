import { useState, type FormEvent } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useCreateDocument, type RoleOption } from "./document-hooks"

const MAX=5*1024*1024
export function UploadDocument({roles}:{roles:RoleOption[]}){
  const upload=useCreateDocument();const [title,setTitle]=useState("");const [category,setCategory]=useState("");const [file,setFile]=useState<File|null>(null);const [selected,setSelected]=useState<string[]>(roles.map(role=>role.code));const [validation,setValidation]=useState("")
  const submit=(event:FormEvent)=>{event.preventDefault();setValidation("");if(!file){setValidation("Choose a document.");return}if(file.size>MAX){setValidation("Documents must be 5 MB or smaller.");return}if(selected.length===0){setValidation("Select at least one role.");return}upload.mutate({metadata:{version:0,title,category,visibleRoleCodes:selected},file},{onSuccess:()=>{setTitle("");setCategory("");setFile(null)}})}
  return <form className="space-y-3 rounded-xl border bg-card p-4" onSubmit={submit}>
    <h2 className="text-xl font-semibold">Upload a document</h2>
    <div className="grid gap-3 md:grid-cols-2"><label>Title<Input required maxLength={200} value={title} onChange={event=>setTitle(event.target.value)}/></label><label>Category<Input required maxLength={80} placeholder="Policy, finance, governance…" value={category} onChange={event=>setCategory(event.target.value)}/></label></div>
    <label>File<Input required type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.txt,.png,.jpg,.jpeg" onChange={event=>setFile(event.target.files?.[0]??null)}/></label>
    <fieldset><legend className="font-medium">Visible to roles</legend><div className="flex flex-wrap gap-3">{roles.map(role=><label className="flex items-center gap-2" key={role.code}><input type="checkbox" checked={selected.includes(role.code)} onChange={event=>setSelected(current=>event.target.checked?[...current,role.code]:current.filter(code=>code!==role.code))}/>{role.name}</label>)}</div></fieldset>
    {(validation||upload.error)&&<p role="alert" className="text-destructive">{validation||errorMessage(upload.error)}</p>}
    <Button disabled={upload.isPending} type="submit">{upload.isPending?"Uploading…":"Upload document"}</Button>
  </form>
}
