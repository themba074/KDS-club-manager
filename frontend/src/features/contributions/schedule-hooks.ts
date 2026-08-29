import { useMutation,useQuery,useQueryClient } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"
export type Frequency="MONTHLY"|"ONCE_OFF"
export type AssignmentMode="ALL_CURRENT"|"SELECTED"
export type ContributionMember={membershipId:string,email:string,displayName:string}
export type ContributionSchedule={scheduleId:string;versionId:string;versionNumber:number;name:string;amount:number;currency:string;frequency:Frequency;firstDueDate:string;endDate:string|null;effectiveFrom:string;effectiveTo:string|null;assignmentMode:AssignmentMode;assignedMembers:ContributionMember[]}
export type ScheduleInput={name:string;amount:string;frequency:Frequency;firstDueDate:string;endDate:string|null;effectiveFrom:string;assignmentMode:AssignmentMode;membershipIds:string[]}
export type ExpectedContribution={scheduleId:string;scheduleVersionId:string;scheduleName:string;membershipId:string;memberEmail:string;memberName:string;dueDate:string;amount:number;currency:string}
const club=()=>useAuthStore.getState().activeClub?.id
export function useContributionSchedules(){const id=useAuthStore(s=>s.activeClub?.id);return useQuery({queryKey:["contribution-schedules",id],enabled:Boolean(id),queryFn:({signal})=>api.get<ContributionSchedule[]>("/contribution-schedules",{signal}).then(r=>r.data)})}
export function useAssignableMembers(enabled:boolean){const id=useAuthStore(s=>s.activeClub?.id);return useQuery({queryKey:["contribution-assignees",id],enabled:Boolean(id)&&enabled,queryFn:({signal})=>api.get<ContributionMember[]>("/contribution-schedules/assignable-members",{signal}).then(r=>r.data)})}
export function useSaveSchedule(){const client=useQueryClient();return useMutation({mutationFn:({scheduleId,input}:{scheduleId?:string;input:ScheduleInput})=>scheduleId?api.put(`/contribution-schedules/${scheduleId}`,input):api.post("/contribution-schedules",input),onSuccess:()=>{void client.invalidateQueries({queryKey:["contribution-schedules",club()]});void client.invalidateQueries({queryKey:["expected-contributions",club()]})}})}
export function useUpcomingContributions(from:string,to:string){const id=useAuthStore(s=>s.activeClub?.id);return useQuery({queryKey:["expected-contributions",id,from,to],enabled:Boolean(id&&from&&to),queryFn:({signal})=>api.get<ExpectedContribution[]>("/contribution-schedules/upcoming",{signal,params:{from,to}}).then(r=>r.data)})}
