import { FileText } from "lucide-react"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { DocumentLibrary } from "./DocumentLibrary"
import { useDocuments } from "./document-hooks"
import { UploadDocument } from "./UploadDocument"

export function DocumentsPage(){const query=useDocuments();return <div className="space-y-6"><div><FileText className="mb-2"/><h1 className="text-3xl font-bold">Documents</h1><p className="text-muted-foreground">Store club files, control role access, and keep every version.</p></div>{query.isPending&&<p role="status">Loading documents…</p>}{query.error&&<div role="alert"><p>{errorMessage(query.error)}</p><Button variant="outline" onClick={()=>void query.refetch()}>Retry documents</Button></div>}{query.data&&<>{query.data.canManage&&<UploadDocument roles={query.data.visibilityRoles}/>}<DocumentLibrary documents={query.data.documents} roles={query.data.visibilityRoles} canManage={query.data.canManage}/></>}</div>}
