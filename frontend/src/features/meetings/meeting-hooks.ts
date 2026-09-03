import { useMutation,useQuery,useQueryClient } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"
export type AgendaItem={id:string;position:number;title:string;description:string|null}
export type Meeting={id:string;version:number;title:string;description:string|null;startsAt:string;durationMinutes:number;location:string|null;meetingUrl:string|null;agendaItems:AgendaItem[]}
export type MeetingInput={version:number;title:string;description:string;startsAt:string;durationMinutes:number;location:string;meetingUrl:string;agendaItems:{title:string;description:string}[]}
export function useMeetings(view:"UPCOMING"|"PAST"){const clubId=useAuthStore(s=>s.activeClub?.id);return useQuery({queryKey:["meetings",clubId,view],enabled:Boolean(clubId),queryFn:({signal})=>api.get<Meeting[]>("/meetings",{signal,params:{view}}).then(r=>r.data)})}
export function useSaveMeeting(){const client=useQueryClient();const clubId=useAuthStore(s=>s.activeClub?.id);return useMutation({mutationFn:({meetingId,input}:{meetingId?:string;input:MeetingInput})=>meetingId?api.put<Meeting>(`/meetings/${meetingId}`,input).then(r=>r.data):api.post<Meeting>("/meetings",input).then(r=>r.data),onSuccess:()=>void client.invalidateQueries({queryKey:["meetings",clubId]})})}
