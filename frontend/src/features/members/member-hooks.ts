import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { api, type AuthResponse } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export type MemberStatus = "INVITED" | "ACTIVE"
export type MemberDirectoryEntry = {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  phone: string | null
  roleCode: string
  status: MemberStatus
  joinedOrInvitedAt: string
}
export type InviteMemberInput = { email: string; firstName: string; lastName: string; phone?: string }
export type InvitationPreview = {
  clubName: string
  email: string
  firstName: string
  lastName: string
  roleCode: string
  accountExists: boolean
  expiresAt: string
}

export function useMembers(search: string, status: MemberStatus | "ALL") {
  const clubId = useAuthStore((state) => state.activeClub?.id)
  return useQuery({
    queryKey: ["members", clubId, search, status],
    enabled: Boolean(clubId),
    queryFn: ({ signal }) => api.get<MemberDirectoryEntry[]>("/members", {
      signal,
      params: { search: search || undefined, status: status === "ALL" ? undefined : status },
    }).then(({ data }) => data),
  })
}

export function useInviteMember() {
  const client = useQueryClient()
  const clubId = useAuthStore((state) => state.activeClub?.id)
  return useMutation({
    mutationFn: (input: InviteMemberInput) => api.post<MemberDirectoryEntry>("/member-invitations", input).then(({ data }) => data),
    onSuccess: () => client.invalidateQueries({ queryKey: ["members", clubId] }),
  })
}

export function useInvitationPreview(token: string | null) {
  return useQuery({
    queryKey: ["member-invitation", token],
    enabled: Boolean(token),
    retry: false,
    queryFn: ({ signal }) => api.get<InvitationPreview>("/member-invitations/accept", { signal, params: { token } }).then(({ data }) => data),
  })
}

export function useAcceptInvitation() {
  const setSession = useAuthStore((state) => state.setSession)
  return useMutation({
    mutationFn: (input: { token: string; password?: string }) =>
      api.post<AuthResponse>("/member-invitations/accept", input).then(({ data }) => data),
    onSuccess: (session) => setSession(session.accessToken, session.user, session.activeClub),
  })
}
