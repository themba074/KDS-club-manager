import { useMutation,useQuery,useQueryClient } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"
export type RsvpResponse="YES"|"NO"|"MAYBE"
export type Rsvp={response:RsvpResponse|null;counts:{yes:number;no:number;maybe:number}|null}
export type Minutes={id:string;version:number;body:string|null;attachmentName:string|null;attachmentSize:number|null;publishedAt:string|null;published:boolean}
function key(meetingId:string,suffix:string){return ["meeting",useAuthStore.getState().activeClub?.id,meetingId,suffix]}
export function useRsvp(meetingId:string,enabled=true){return useQuery({queryKey:key(meetingId,"rsvp"),enabled,queryFn:()=>api.get<Rsvp>(`/meetings/${meetingId}/rsvp`).then(r=>r.data)})}
export function useRespond(meetingId:string){const client=useQueryClient();return useMutation({mutationFn:(response:RsvpResponse)=>api.put<Rsvp>(`/meetings/${meetingId}/rsvp`,{response}).then(r=>r.data),onSuccess:data=>client.setQueryData(key(meetingId,"rsvp"),data)})}
export function useMinutes(meetingId:string,enabled=true){return useQuery({queryKey:key(meetingId,"minutes"),enabled,retry:false,queryFn:()=>api.get<Minutes>(`/meetings/${meetingId}/minutes`).then(r=>r.data)})}
export function useSaveMinutes(meetingId:string){const client=useQueryClient();return useMutation({mutationFn:({version,body}:{version:number;body:string})=>api.put<Minutes>(`/meetings/${meetingId}/minutes`,{version,body}).then(r=>r.data),onSuccess:data=>client.setQueryData(key(meetingId,"minutes"),data)})}
export function useAttachMinutes(meetingId:string){const client=useQueryClient();return useMutation({mutationFn:({version,file}:{version:number;file:File})=>{const data=new FormData();data.append("file",file);return api.post<Minutes>(`/meetings/${meetingId}/minutes/attachment`,data,{params:{version}}).then(r=>r.data)},onSuccess:data=>client.setQueryData(key(meetingId,"minutes"),data)})}
export function usePublishMinutes(meetingId:string){const client=useQueryClient();return useMutation({mutationFn:(version:number)=>api.post<Minutes>(`/meetings/${meetingId}/minutes/publish`,{version}).then(r=>r.data),onSuccess:data=>client.setQueryData(key(meetingId,"minutes"),data)})}
