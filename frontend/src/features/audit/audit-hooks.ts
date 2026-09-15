import { useQuery } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export const auditActions = ["PAYMENT_RECORDED","VOTE_CAST","VOTE_RESULTS_PUBLISHED","ROLE_ASSIGNED","DOCUMENT_UPLOADED","DOCUMENT_METADATA_UPDATED","DOCUMENT_VERSION_UPLOADED","MEMBER_STATUS_CHANGED"] as const
export type AuditAction = typeof auditActions[number]
export type AuditEntry = { id:string;actorId:string;action:AuditAction;entityType:string;entityId:string;previousValue:string|null;newValue:string|null;occurredAt:string }
export type AuditPage = { content:AuditEntry[];totalElements:number;totalPages:number;page:number;size:number }
export type AuditFilters = { actor:string;action:AuditAction|"";from:string;to:string;page:number }

export function useAuditLog(filters:AuditFilters){
  const clubId=useAuthStore(state=>state.activeClub?.id)
  const actor=filters.actor.trim()
  const validActor=!actor||/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(actor)
  return useQuery({queryKey:["audit-log",clubId,filters],enabled:Boolean(clubId&&validActor),queryFn:({signal})=>api.get<AuditPage>("/audit-log",{signal,params:{actor:actor||undefined,action:filters.action||undefined,from:filters.from||undefined,to:filters.to||undefined,page:filters.page,size:25}}).then(response=>response.data)})
}
