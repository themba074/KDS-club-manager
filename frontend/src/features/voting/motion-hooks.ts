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
  selectedOptionId: string | null;
  resultsPublished: boolean;
};
export type MotionResult = {
  motionId: string;
  published: boolean;
  publishedAt: string | null;
  totalVotes: number;
  eligibleVoterCount: number;
  outcome: "WINNER" | "NO_MAJORITY";
  winningOptionId: string | null;
  options: {
    optionId: string;
    position: number;
    label: string;
    voteCount: number;
  }[];
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

export function useCastVote() {
  const client = useQueryClient();
  const clubId = useAuthStore((state) => state.activeClub?.id);
  return useMutation({
    mutationFn: ({
      motionId,
      optionId,
    }: {
      motionId: string;
      optionId: string;
    }) =>
      api
        .post(`/motions/${motionId}/votes`, { optionId })
        .then((response) => response.data),
    onSuccess: (_, variables) => {
      client.setQueryData<Motion[]>(["motions", clubId], (current) =>
        current?.map((motion) =>
          motion.id === variables.motionId
            ? { ...motion, selectedOptionId: variables.optionId }
            : motion,
        ),
      );
    },
  });
}

export function useMotionResults(motionId: string, enabled: boolean) {
  const clubId = useAuthStore((state) => state.activeClub?.id);
  return useQuery({
    queryKey: ["motion-results", clubId, motionId],
    enabled: Boolean(clubId) && enabled,
    queryFn: ({ signal }) =>
      api
        .get<MotionResult>(`/motions/${motionId}/results`, { signal })
        .then((response) => response.data),
  });
}

export function usePublishMotionResults(motionId: string) {
  const client = useQueryClient();
  const clubId = useAuthStore((state) => state.activeClub?.id);
  return useMutation({
    mutationFn: (version: number) =>
      api
        .post<MotionResult>(`/motions/${motionId}/results/publish`, { version })
        .then((response) => response.data),
    onSuccess: (result) => {
      client.setQueryData(["motion-results", clubId, motionId], result);
      void client.invalidateQueries({ queryKey: ["motions", clubId] });
    },
  });
}
