import { Button } from "@/components/ui/button";
import { errorMessage } from "@/features/auth/auth-api";
import {
  useMotionResults,
  usePublishMotionResults,
  type Motion,
} from "./motion-hooks";

export function MotionResults({
  motion,
  canPublish,
}: {
  motion: Motion;
  canPublish: boolean;
}) {
  const visible = motion.resultsPublished || canPublish;
  const results = useMotionResults(motion.id, visible);
  const publish = usePublishMotionResults(motion.id);

  if (!visible) return <p>Results will be available after publication.</p>;
  if (results.isPending) return <p role="status">Loading results…</p>;
  if (results.error) return <p role="alert">{errorMessage(results.error)}</p>;
  if (!results.data) return null;

  const winner = results.data.options.find(
    (option) => option.optionId === results.data?.winningOptionId,
  );
  return (
    <section className="space-y-2 rounded-lg border p-3">
      <h4 className="font-semibold">
        {results.data.published ? "Published results" : "Result preview"}
      </h4>
      <p>
        {results.data.totalVotes} of {results.data.eligibleVoterCount} eligible
        members voted.
      </p>
      <ol className="space-y-1">
        {results.data.options.map((option) => (
          <li className="flex justify-between gap-3" key={option.optionId}>
            <span>{option.label}</span>
            <span>
              {option.voteCount} vote{option.voteCount === 1 ? "" : "s"}
            </span>
          </li>
        ))}
      </ol>
      <p className="font-medium">
        {winner
          ? `Winner: ${winner.label}`
          : "No option received a simple majority."}
      </p>
      {results.data.publishedAt && (
        <p className="text-sm text-muted-foreground">
          Published {new Date(results.data.publishedAt).toLocaleString()}
        </p>
      )}
      {!results.data.published && canPublish && (
        <>
          <p className="text-sm text-muted-foreground">
            Publishing freezes this result permanently.
          </p>
          <Button
            type="button"
            disabled={publish.isPending}
            onClick={() => publish.mutate(motion.version)}
          >
            {publish.isPending ? "Publishing…" : "Publish results"}
          </Button>
        </>
      )}
      {publish.error && <p role="alert">{errorMessage(publish.error)}</p>}
    </section>
  );
}
