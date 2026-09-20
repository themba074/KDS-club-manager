import { useConfirmation } from "@/components/ui/use-confirmation";
import { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/features/auth/auth-api";
import { api } from "@/features/auth/auth-api";
import {
  useAttachMinutes,
  useMinutes,
  usePublishMinutes,
  useSaveMinutes,
} from "./participation-hooks";
function status(error: unknown) {
  return typeof error === "object" && error !== null && "response" in error
    ? (error as { response?: { status?: number } }).response?.status
    : undefined;
}
export function MinutesEditor({
  meetingId,
  canWrite,
  past,
}: {
  meetingId: string;
  canWrite: boolean;
  past: boolean;
}) {
  const query = useMinutes(meetingId);
  const save = useSaveMinutes(meetingId);
  const attach = useAttachMinutes(meetingId);
  const publish = usePublishMinutes(meetingId);
  const body = useRef<HTMLTextAreaElement>(null);
  const { confirm, confirmation } = useConfirmation();
  const [file, setFile] = useState<File | null>(null);
  const [validation, setValidation] = useState("");
  const fileInput = useRef<HTMLInputElement>(null);
  const busy = save.isPending || attach.isPending || publish.isPending;
  const unsaved = () =>
    body.current !== null && body.current.value !== (query.data?.body ?? "");
  const upload = () => {
    setValidation("");
    if (!file || busy) return;
    if (unsaved()) {
      setValidation("Save your notes before attaching a file.");
      return;
    }
    if (
      file.size === 0 ||
      file.size > 5 * 1024 * 1024 ||
      ![
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
      ].includes(file.type)
    ) {
      setValidation(
        "Choose a non-empty PDF, DOCX, or text file no larger than 5 MB.",
      );
      return;
    }
    confirm(
      "Attach minutes file",
      `Attach ${file.name}? This replaces any current attachment and returns published minutes to draft.`,
      () =>
        attach.mutate(
          { version, file },
          {
            onSuccess: () => {
              setFile(null);
              if (fileInput.current) fileInput.current.value = "";
            },
          },
        ),
    );
  };
  const missing = status(query.error) === 404 || status(query.error) === 403;
  const version = query.data?.version ?? 0;
  const download = async () => {
    setValidation("");
    try {
      const response = await api.get(
        `/meetings/${meetingId}/minutes/attachment`,
        { responseType: "blob" },
      );
      const url = URL.createObjectURL(response.data);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = query.data?.attachmentName ?? "meeting-minutes";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      setValidation(errorMessage(error));
    }
  };
  return (
    <section className="space-y-2" aria-label="Meeting minutes">
      {confirmation}
      <h4 className="font-medium">Minutes</h4>
      {validation && <p role="alert">{validation}</p>}
      {query.isPending && <p role="status">Loading minutes…</p>}
      {query.error && !missing && (
        <p role="alert">{errorMessage(query.error)}</p>
      )}
      {missing && !canWrite && <p>Minutes have not been published.</p>}
      {query.data?.publishedAt && (
        <p className="text-sm text-muted-foreground">
          Published{" "}
          {new Intl.DateTimeFormat(undefined, {
            dateStyle: "medium",
            timeStyle: "short",
          }).format(new Date(query.data.publishedAt))}
        </p>
      )}
      {canWrite && past && (
        <>
          <label className="grid gap-1">
            Meeting notes
            <textarea
              key={`${query.data?.id ?? "new"}-${version}`}
              ref={body}
              disabled={busy}
              className="min-h-40 rounded-md border bg-background p-3"
              maxLength={20000}
              defaultValue={query.data?.body ?? ""}
              placeholder="Record decisions, actions, and discussion notes…"
            />
          </label>
          <p className="text-xs text-muted-foreground">
            Use clear headings and lists; text is displayed safely without
            executing HTML.
          </p>
          <Button
            type="button"
            disabled={busy || query.isPending}
            onClick={() => {
              setValidation("");
              const notes = body.current?.value ?? "";
              const persist = () => save.mutate({ version, body: notes });
              if (query.data?.published)
                confirm(
                  "Return minutes to draft",
                  "Saving these notes will hide the published minutes from members until you publish them again.",
                  persist,
                );
              else persist();
            }}
          >
            Save draft
          </Button>
          <label className="block text-sm">
            Attach PDF, DOCX, or text file (maximum 5 MB)
            <input
              className="mt-1 block"
              ref={fileInput}
              disabled={busy}
              type="file"
              accept=".pdf,.docx,.txt"
              onChange={(event) => {
                setFile(event.target.files?.[0] ?? null);
                setValidation("");
              }}
            />
          </label>
          <Button
            type="button"
            disabled={!file || busy || query.isPending}
            onClick={upload}
          >
            Upload attachment
          </Button>
          {file && (
            <Button
              type="button"
              variant="outline"
              disabled={busy}
              onClick={() => {
                setFile(null);
                setValidation("");
                if (fileInput.current) fileInput.current.value = "";
              }}
            >
              Clear selected attachment
            </Button>
          )}
          {query.data && !query.data.published && (
            <Button
              type="button"
              variant="outline"
              disabled={busy}
              onClick={() => {
                setValidation("");
                if (unsaved() || file) {
                  setValidation(
                    "Save your notes and upload or clear the selected attachment before publishing.",
                  );
                  return;
                }
                confirm(
                  "Publish meeting minutes",
                  "Publish the saved notes and attachment? Club members will be able to read them and receive a notification.",
                  () => publish.mutate(query.data.version),
                );
              }}
            >
              Publish minutes
            </Button>
          )}
        </>
      )}
      {query.data?.body && (!canWrite || !past) && (
        <p className="whitespace-pre-wrap rounded-md bg-muted p-3">
          {query.data.body}
        </p>
      )}
      {query.data?.attachmentName && (
        <Button type="button" variant="outline" onClick={() => void download()}>
          Download {query.data.attachmentName}
        </Button>
      )}
      {(save.error || attach.error || publish.error) && (
        <p role="alert">
          {errorMessage(save.error || attach.error || publish.error)}
        </p>
      )}
    </section>
  );
}
