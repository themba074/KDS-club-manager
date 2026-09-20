import { useState } from "react";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { errorMessage } from "@/features/auth/auth-api";
import { useMembers } from "@/features/members/member-hooks";
import { EmptyState } from "@/components/states/EmptyState";
import { ErrorState } from "@/components/states/ErrorState";
import { UsersRound } from "lucide-react";
import { useSaveMotion, type Motion } from "./motion-hooks";

type Form = {
  title: string;
  description: string;
  opensAt: string;
  closesAt: string;
  options: { label: string }[];
  all: boolean;
  eligible: string[];
};
function localTime(value: Date) {
  const local = new Date(value.getTime() - value.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}
function defaults(motion?: Motion): Form {
  return {
    title: motion?.title ?? "",
    description: motion?.description ?? "",
    opensAt: localTime(new Date(motion?.opensAt ?? Date.now() + 3_600_000)),
    closesAt: localTime(new Date(motion?.closesAt ?? Date.now() + 86_400_000)),
    options: motion?.options.map((option) => ({ label: option.label })) ?? [
      { label: "" },
      { label: "" },
    ],
    // An edit preserves the explicit snapshot; it does not silently add later joiners.
    all: !motion,
    eligible: motion?.eligibleMembershipIds ?? [],
  };
}

export function CreateMotion({
  motion,
  onDone,
}: {
  motion?: Motion;
  onDone?: () => void;
}) {
  const save = useSaveMotion();
  const members = useMembers("", "ACTIVE");
  const [initialValues] = useState(() => defaults(motion));
  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<Form>({ defaultValues: initialValues });
  const fields = useFieldArray({ control, name: "options" });
  const all = useWatch({ control, name: "all" });
  const selected = useWatch({ control, name: "eligible" });
  const unavailable = members.data
    ? selected.filter((id) => !members.data.some((member) => member.id === id))
    : [];
  const submit = (values: Form) => {
    const opening = new Date(values.opensAt);
    const closing = new Date(values.closesAt);
    if (
      !Number.isFinite(opening.getTime()) ||
      !Number.isFinite(closing.getTime()) ||
      closing <= opening ||
      (motion?.state !== "CANCELLED" && opening.getTime() <= Date.now())
    ) {
      setError("root", {
        message:
          "Choose a future opening time and a later closing time. Cancelled motions may retain past dates.",
      });
      return;
    }
    const options = values.options.map((option) => option.label.trim());
    if (
      options.some((option) => !option) ||
      new Set(options.map((option) => option.toLowerCase())).size !==
        options.length
    ) {
      setError("root", { message: "Provide distinct, non-empty options." });
      return;
    }
    if (!values.all && values.eligible.length === 0) {
      setError("root", { message: "Select at least one eligible member." });
      return;
    }
    save.mutate(
      {
        id: motion?.id,
        input: {
          version: motion?.version ?? 0,
          title: values.title.trim(),
          description: values.description.trim(),
          opensAt: opening.toISOString(),
          closesAt: closing.toISOString(),
          options,
          eligibleMembershipIds: values.eligible,
          allActiveMembers: values.all,
        },
      },
      {
        onSuccess: () => {
          if (motion) onDone?.();
          else reset(defaults());
        },
      },
    );
  };
  return (
    <form
      className="space-y-3 rounded-xl border bg-card p-4"
      onSubmit={(event) => void handleSubmit(submit)(event)}
    >
      <h2 className="text-xl font-semibold">
        {motion ? "Edit motion" : "Create motion"}
      </h2>
      {motion?.state === "CANCELLED" && (
        <p>This motion will remain cancelled after saving.</p>
      )}
      <fieldset disabled={save.isPending} className="space-y-3">
        <label className="block">
          Title
          <Input
            required
            maxLength={200}
            {...register("title", {
              required: true,
              validate: (value) => Boolean(value.trim()),
            })}
          />
        </label>
        <label className="block">
          Description
          <textarea
            maxLength={4000}
            className="w-full rounded-md border p-2"
            {...register("description")}
          />
        </label>
        <div className="grid gap-3 sm:grid-cols-2">
          <label>
            Opens at
            <Input
              required
              type="datetime-local"
              {...register("opensAt", { required: true })}
            />
          </label>
          <label>
            Closes at
            <Input
              required
              type="datetime-local"
              {...register("closesAt", { required: true })}
            />
          </label>
        </div>
        <fieldset className="space-y-2">
          <legend>Options</legend>
          {fields.fields.map((field, index) => (
            <div className="flex gap-2" key={field.id}>
              <Input
                required
                maxLength={200}
                aria-label={`Option ${index + 1}`}
                {...register(`options.${index}.label`, { required: true })}
              />
              {fields.fields.length > 2 && (
                <Button
                  type="button"
                  variant="outline"
                  aria-label={`Remove option ${index + 1}`}
                  onClick={() => fields.remove(index)}
                >
                  Remove
                </Button>
              )}
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            disabled={fields.fields.length >= 20}
            onClick={() => fields.append({ label: "" })}
          >
            Add option
          </Button>
        </fieldset>
        <label className="flex gap-2">
          <input type="checkbox" {...register("all")} /> All active members may
          vote
        </label>
        <p className="text-sm text-muted-foreground">
          Eligibility is saved as a snapshot. Members joining later are not
          added automatically.
        </p>
        {!all && (
          <fieldset>
            <legend>Select eligible members</legend>
            {members.isPending && <p role="status">Loading members…</p>}
            {members.error && <ErrorState title="We couldn't load eligible members" description={errorMessage(members.error, "Try loading the active member list again.")} onRetry={() => void members.refetch()} />}
            {members.data?.length === 0 && <EmptyState icon={UsersRound} title="No active members can be selected" description="Invite or reactivate members before creating a motion for a selected group." />}
            {members.data?.map((member) => (
              <label className="block" key={member.id}>
                <input
                  type="checkbox"
                  value={member.id}
                  {...register("eligible")}
                />{" "}
                {member.firstName || member.email} {member.lastName || ""}
              </label>
            ))}
            {unavailable.map((id, index) => (
              <label className="block" key={id}>
                <input type="checkbox" value={id} {...register("eligible")} />{" "}
                Unavailable selected member {index + 1} — deselect before saving
              </label>
            ))}
          </fieldset>
        )}
        <div className="flex flex-wrap gap-2">
          <Button
            type="submit"
            disabled={!all && (members.isPending || members.isError)}
          >
            {save.isPending
              ? "Saving…"
              : motion
                ? "Save motion"
                : "Create motion"}
          </Button>
          {motion && (
            <Button type="button" variant="outline" onClick={onDone}>
              Discard edits
            </Button>
          )}
        </div>
      </fieldset>
      {Object.keys(errors).length > 0 && (
        <p role="alert">
          {errors.root?.message ?? "Complete the required fields."}
        </p>
      )}
      {save.error && <p role="alert">{errorMessage(save.error)}</p>}
      {save.isSuccess && !motion && <p role="status">Motion created.</p>}
    </form>
  );
}
