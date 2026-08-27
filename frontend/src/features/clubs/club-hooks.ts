import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import axios from "axios"
import { api, selectClubSession } from "@/features/auth/auth-api"
import { useAuthStore, type ClubSummary } from "@/features/auth/auth-store"

export function useClubs() {
  const userId = useAuthStore((state) => state.user?.id)
  const switching = useAuthStore((state) => state.switchingClub)
  return useQuery({
    queryKey: ["clubs", userId],
    enabled: Boolean(userId) && !switching,
    queryFn: ({ signal }) => api.get<ClubSummary[]>("/clubs", { signal }).then(({ data }) => data),
  })
}

export function useCreateClub() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.post<ClubSummary>("/clubs", { name, clubType: "INVESTMENT_CLUB" }).then(({ data }) => data),
    onSuccess: () => { void queryClient.invalidateQueries({ queryKey: ["clubs"] }) },
  })
}

export function useSelectClub() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (clubId: string) => {
      const version = useAuthStore.getState().beginClubSwitch()
      try {
        await queryClient.cancelQueries()
        queryClient.removeQueries()
        return await selectClubSession(clubId, version)
      } catch (error) {
        if (axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) {
          useAuthStore.getState().clearSession()
        }
        throw error
      } finally {
        useAuthStore.getState().endClubSwitch()
      }
    },
  })
}

// All subsequent tenant features must include activeClub.id in their query keys.
export function useClubContext() {
  return useAuthStore((state) => state.activeClub)
}
