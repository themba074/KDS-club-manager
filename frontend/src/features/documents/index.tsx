import { FileText } from "lucide-react"
import { ErrorState } from "@/components/states/ErrorState"
import { errorMessage } from "@/features/auth/auth-api"
import { DocumentLibrary } from "./DocumentLibrary"
import { useDocuments } from "./document-hooks"
import { UploadDocument } from "./UploadDocument"

export function DocumentsPage(){const query=useDocuments();return <div className="space-y-6"><div><FileText className="mb-2"/><h1 className="text-3xl font-bold">Documents</h1><p className="text-muted-foreground">Store club files, control role access, and keep every version.</p></div>{query.isPending&&<p role="status">Loading documents…</p>}{query.error&&<ErrorState title="We couldn't load club documents" description={errorMessage(query.error, "Try loading the document library again.")} onRetry={()=>void query.refetch()}/>} {query.data&&<>{query.data.canManage&&<UploadDocument roles={query.data.visibilityRoles}/>}<DocumentLibrary documents={query.data.documents} roles={query.data.visibilityRoles} canManage={query.data.canManage}/></>}</div>}
