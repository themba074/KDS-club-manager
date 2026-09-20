import { useConfirmation } from "@/components/ui/use-confirmation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/features/auth/auth-api";
import { useCastVote, type Motion } from "./motion-hooks";

export function CastVote({ motion }: { motion: Motion }) {
  const [optionId, setOptionId] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const cast = useCastVote();
  const { confirm, confirmation } = useConfirmation();

  if (motion.selectedOptionId) {
    const selected = motion.options.find(
      (option) => option.id === motion.selectedOptionId,
    );
    return (
      <p role="status">
        Vote recorded{selected ? ` for ${selected.label}` : ""}. Votes cannot be
        changed.
      </p>
    );
  }

  const submit = () => {
    if (!optionId) {
      setValidationError("Choose an option before casting your vote.");
      return;
    }
    setValidationError(null);
    confirm(
      "Confirm your vote",
      `Vote for ${motion.options.find((option) => option.id === optionId)?.label} on “${motion.title}”? Your vote cannot be changed.`,
      () => cast.mutate({ motionId: motion.id, optionId }),
    );
  };

  return (
    <fieldset className="space-y-2 rounded-lg border p-3">
      <legend className="font-medium">Cast your vote</legend>
      {confirmation}
      <p className="text-sm text-muted-foreground">
        Your choice is final and cannot be changed.
      </p>
      {motion.options.map((option) => (
        <label className="flex items-center gap-2" key={option.id}>
          <input
            type="radio"
            name={`motion-${motion.id}`}
            value={option.id}
            checked={optionId === option.id}
            disabled={cast.isPending}
            onChange={() => setOptionId(option.id)}
          />
          {option.label}
        </label>
      ))}
      <Button type="button" disabled={cast.isPending} onClick={submit}>
        {cast.isPending ? "Casting vote…" : "Cast vote"}
      </Button>
      {(validationError || cast.error) && (
        <p role="alert">{validationError ?? errorMessage(cast.error)}</p>
      )}
    </fieldset>
  );
}
