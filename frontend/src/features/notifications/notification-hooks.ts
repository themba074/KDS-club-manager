import { useMutation,useQuery,useQueryClient } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export type NotificationType="PAYMENT_REMINDER"|"MEETING_SCHEDULED"|"MEETING_UPDATED"|"VOTE_OPEN"|"VOTE_CLOSING"|"MINUTES_PUBLISHED"
export type ClubNotification={id:string;type:NotificationType;title:string;message:string;targetPath:string|null;availableAt:string;readAt:string|null}
const club=()=>useAuthStore.getState().activeClub?.id
export function useNotifications(){const id=useAuthStore(state=>state.activeClub?.id);return useQuery({queryKey:["notifications",id],enabled:Boolean(id),queryFn:({signal})=>api.get<ClubNotification[]>("/notifications",{signal}).then(response=>response.data),refetchInterval:30_000})}
export function useUnreadNotifications(){const id=useAuthStore(state=>state.activeClub?.id);return useQuery({queryKey:["notifications-unread",id],enabled:Boolean(id),queryFn:({signal})=>api.get<{count:number}>("/notifications/unread-count",{signal}).then(response=>response.data.count),refetchInterval:30_000})}
function invalidations(client:ReturnType<typeof useQueryClient>){void client.invalidateQueries({queryKey:["notifications",club()]});void client.invalidateQueries({queryKey:["notifications-unread",club()]})}
export function useMarkNotificationRead(){const client=useQueryClient();return useMutation({mutationFn:(id:string)=>api.put(`/notifications/${id}/read`),onSuccess:()=>invalidations(client)})}
export function useMarkAllNotificationsRead(){const client=useQueryClient();return useMutation({mutationFn:()=>api.put("/notifications/read-all"),onSuccess:()=>invalidations(client)})}
