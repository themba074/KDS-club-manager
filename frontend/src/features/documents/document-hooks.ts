import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export type DocumentVersion={id:string;versionNumber:number;fileName:string;contentType:string;fileSize:number;uploadedBy:string;uploadedAt:string}
export type ClubDocument={id:string;title:string;category:string;visibleRoleCodes:string[];createdBy:string;createdAt:string;updatedAt:string;version:number;versions:DocumentVersion[]}
export type RoleOption={code:string;name:string}
export type DocumentLibrary={documents:ClubDocument[];visibilityRoles:RoleOption[];canManage:boolean}
export type DocumentMetadata={version:number;title:string;category:string;visibleRoleCodes:string[]}

function form(metadata:DocumentMetadata,file:File){const data=new FormData();data.append("metadata",new Blob([JSON.stringify(metadata)],{type:"application/json"}));data.append("file",file);return data}
export function useDocuments(){const clubId=useAuthStore(state=>state.activeClub?.id);return useQuery({queryKey:["documents",clubId],enabled:Boolean(clubId),queryFn:({signal})=>api.get<DocumentLibrary>("/documents",{signal}).then(response=>response.data)})}
export function useCreateDocument(){const client=useQueryClient();const clubId=useAuthStore(state=>state.activeClub?.id);return useMutation({mutationFn:({metadata,file}:{metadata:DocumentMetadata;file:File})=>api.post<ClubDocument>("/documents",form(metadata,file)).then(response=>response.data),onSuccess:()=>void client.invalidateQueries({queryKey:["documents",clubId]})})}
export function useUpdateDocument(){const client=useQueryClient();const clubId=useAuthStore(state=>state.activeClub?.id);return useMutation({mutationFn:({id,metadata}:{id:string;metadata:DocumentMetadata})=>api.put<ClubDocument>(`/documents/${id}`,metadata).then(response=>response.data),onSuccess:()=>void client.invalidateQueries({queryKey:["documents",clubId]})})}
export function useReplaceDocument(){const client=useQueryClient();const clubId=useAuthStore(state=>state.activeClub?.id);return useMutation({mutationFn:({id,version,file}:{id:string;version:number;file:File})=>{const data=new FormData();data.append("file",file);return api.post<ClubDocument>(`/documents/${id}/versions`,data,{params:{version}}).then(response=>response.data)},onSuccess:()=>void client.invalidateQueries({queryKey:["documents",clubId]})})}
export async function downloadDocument(document:ClubDocument,version:DocumentVersion){const response=await api.get(`/documents/${document.id}/versions/${version.id}/download`,{responseType:"blob"});const url=URL.createObjectURL(response.data);const anchor=window.document.createElement("a");anchor.href=url;anchor.download=version.fileName;anchor.click();URL.revokeObjectURL(url)}
