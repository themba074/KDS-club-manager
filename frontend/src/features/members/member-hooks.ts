import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { api, type AuthResponse } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export type MemberStatus = "INVITED" | "ACTIVE" | "SUSPENDED" | "EXITED"
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
export type ImportColumnMapping = {
  emailColumn: string
  firstNameColumn: string
  lastNameColumn: string
  phoneColumn?: string
}
export type ImportInspection = { headers: string[]; rowCount: number; sampleRows: Record<string, string>[] }
export type ImportRowStatus = "READY" | "INVALID" | "INVITED" | "FAILED"
export type ImportRow = {
  rowNumber: number
  email: string
  firstName: string
  lastName: string
  phone: string | null
  status: ImportRowStatus
  errors: string[]
}
export type ImportPreview = { totalRows: number; readyRows: number; invalidRows: number; rows: ImportRow[] }
export type ImportConfirmation = { totalRows: number; invitedRows: number; failedRows: number; rows: ImportRow[] }

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

export function useChangeMemberStatus() {
  const client = useQueryClient()
  const clubId = useAuthStore((state) => state.activeClub?.id)
  return useMutation({
    mutationFn: ({ membershipId, status }: { membershipId: string; status: Exclude<MemberStatus, "INVITED"> }) =>
      api.patch(`/members/${membershipId}/status`, { status }),
    onSuccess: () => client.invalidateQueries({ queryKey: ["members", clubId] }),
  })
}

function importForm(file: File, mapping?: ImportColumnMapping) {
  const form = new FormData()
  form.append("file", file)
  if (mapping) {
    form.append("emailColumn", mapping.emailColumn)
    form.append("firstNameColumn", mapping.firstNameColumn)
    form.append("lastNameColumn", mapping.lastNameColumn)
    if (mapping.phoneColumn) form.append("phoneColumn", mapping.phoneColumn)
  }
  return form
}

export function useInspectMemberImport() {
  return useMutation({
    mutationFn: (file: File) => api.post<ImportInspection>("/member-imports/inspect", importForm(file)).then(({ data }) => data),
  })
}

export function usePreviewMemberImport() {
  return useMutation({
    mutationFn: ({ file, mapping }: { file: File; mapping: ImportColumnMapping }) =>
      api.post<ImportPreview>("/member-imports/preview", importForm(file, mapping)).then(({ data }) => data),
  })
}

export function useConfirmMemberImport() {
  const client = useQueryClient()
  const clubId = useAuthStore((state) => state.activeClub?.id)
  return useMutation({
    mutationFn: ({ file, mapping }: { file: File; mapping: ImportColumnMapping }) =>
      api.post<ImportConfirmation>("/member-imports/confirm", importForm(file, mapping)).then(({ data }) => data),
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
