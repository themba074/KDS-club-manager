import { useConfirmation } from "@/components/ui/use-confirmation";
import { useState } from "react";
import { Vote } from "lucide-react";
import { EmptyState } from "@/components/states/EmptyState";
import { ErrorState } from "@/components/states/ErrorState";
import { usePermission } from "@/features/roles/use-permission";
import { useAuthStore } from "@/features/auth/auth-store";
import { errorMessage } from "@/features/auth/auth-api";
import { Button } from "@/components/ui/button";
import { CreateMotion } from "./CreateMotion";
import { CastVote } from "./CastVote";
import { MotionResults } from "./MotionResults";
import { useCancelMotion, useMotions, type Motion } from "./motion-hooks";

function MotionCard({
  motion,
  canCreate,
  canCast,
}: {
  motion: Motion;
  canCreate: boolean;
  canCast: boolean;
}) {
  const cancel = useCancelMotion();
  const { confirm, confirmation } = useConfirmation();
  const [editing, setEditing] = useState<Motion | null>(null);
  const canEdit = motion.state === "DRAFT" || motion.state === "CANCELLED";
  return (
    <article className="space-y-3 rounded-xl border bg-card p-4">
      {confirmation}
      <div className="flex justify-between gap-2">
        <h3 className="font-semibold">{motion.title}</h3>
        <span>{motion.state}</span>
      </div>
      {motion.description && (
        <p className="whitespace-pre-wrap break-words">{motion.description}</p>
      )}
      <p className="text-sm">
        Opens {new Date(motion.opensAt).toLocaleString()} · closes{" "}
        {new Date(motion.closesAt).toLocaleString()}
      </p>
      <ol className="list-decimal pl-5">
        {motion.options.map((option) => (
          <li className="break-words" key={option.id}>
            {option.label}
          </li>
        ))}
      </ol>
      <p className="text-sm">
        {motion.eligibleVoterCount} eligible members
        {motion.eligibleToVote && motion.state === "OPEN"
          ? " · You are eligible to vote"
          : ""}
      </p>
      {motion.state === "OPEN" && !motion.eligibleToVote && (
        <p>You are not eligible to vote on this motion.</p>
      )}
      {motion.state === "OPEN" && motion.eligibleToVote && canCast && (
        <CastVote motion={motion} />
      )}
      {motion.state === "CLOSED" && (
        <MotionResults motion={motion} canPublish={canCreate} />
      )}
      {canCreate && (
        <div className="flex flex-wrap gap-2">
          {canEdit && !editing && (
            <Button
              type="button"
              variant="outline"
              onClick={() => setEditing(motion)}
            >
              Edit motion
            </Button>
          )}
          {motion.state !== "CANCELLED" && !motion.resultsPublished && (
            <Button
              type="button"
              variant="outline"
              disabled={cancel.isPending}
              onClick={() =>
                confirm(
                  "Cancel this motion",
                  `Cancel “${motion.title}”? Members will no longer be able to vote on it.`,
                  () =>
                    cancel.mutate(
                      { id: motion.id, version: motion.version },
                      { onSuccess: () => setEditing(null) },
                    ),
                )
              }
            >
              {cancel.isPending ? "Cancelling…" : "Cancel motion"}
            </Button>
          )}
        </div>
      )}
      {cancel.error && <p role="alert">{errorMessage(cancel.error)}</p>}
      {canCreate && canEdit && editing && (
        <CreateMotion motion={editing} onDone={() => setEditing(null)} />
      )}
    </article>
  );
}

export function VotingPage() {
  const canCreate = usePermission("VOTES_CREATE");
  const canCast = usePermission("VOTES_CAST");
  const clubId = useAuthStore((state) => state.activeClub?.id);
  const motions = useMotions();
  return (
    <div className="space-y-6">
      <div>
        <Vote className="mb-2" />
        <h1 className="text-3xl font-bold">Voting</h1>
        <p className="text-muted-foreground">
          Create motions, cast one-time ballots, and publish final results.
        </p>
      </div>
      {canCreate && <CreateMotion key={clubId} />}
      {motions.isPending && <p role="status">Loading motions…</p>}
      {motions.error && (
        <ErrorState title="We couldn't load club motions" description={errorMessage(motions.error, "Try loading the voting list again.")} onRetry={() => void motions.refetch()} />
      )}
      <section className="space-y-3">
        <h2 className="text-xl font-semibold">Motions</h2>
        {motions.data?.length === 0 && <EmptyState icon={Vote} title="No motions yet" description={canCreate?"Use the form above when the club needs to discuss and vote on a decision.":"When an authorised member creates a motion, it will appear here."} />}
        {motions.data?.map((motion) => (
          <MotionCard
            key={`${clubId}:${motion.id}`}
            motion={motion}
            canCreate={canCreate}
            canCast={canCast}
          />
        ))}
      </section>
    </div>
  );
}
