import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/features/auth/auth-api";
import { useAuthStore } from "@/features/auth/auth-store";

export type Motion = {
  id: string;
  version: number;
  title: string;
  description: string | null;
  opensAt: string;
  closesAt: string;
  state: "DRAFT" | "OPEN" | "CLOSED" | "CANCELLED";
  options: { id: string; position: number; label: string }[];
  eligibleVoterCount: number;
  eligibleToVote: boolean;
  eligibleMembershipIds: string[];
};
export type MotionInput = {
  version: number;
  title: string;
  description: string;
  opensAt: string;
  closesAt: string;
  options: string[];
  eligibleMembershipIds: string[];
  allActiveMembers: boolean;
};
export function useMotions() {
  const clubId = useAuthStore((state) => state.activeClub?.id);
  return useQuery({
    queryKey: ["motions", clubId],
    enabled: Boolean(clubId),
    refetchInterval: 15_000,
    queryFn: ({ signal }) =>
      api
        .get<Motion[]>("/motions", { signal })
        .then((response) => response.data),
  });
}
export function useSaveMotion() {
  const client = useQueryClient();
  const clubId = useAuthStore((state) => state.activeClub?.id);
  return useMutation({
    mutationFn: ({ id, input }: { id?: string; input: MotionInput }) =>
      id
        ? api
            .put<Motion>(`/motions/${id}`, input)
            .then((response) => response.data)
        : api.post<Motion>("/motions", input).then((response) => response.data),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: ["motions", clubId] }),
  });
}
export function useCancelMotion() {
  const client = useQueryClient();
  const clubId = useAuthStore((state) => state.activeClub?.id);
  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      api
        .post<Motion>(`/motions/${id}/cancel`, { version })
        .then((response) => response.data),
    onSuccess: () =>
      client.invalidateQueries({ queryKey: ["motions", clubId] }),
  });
}
