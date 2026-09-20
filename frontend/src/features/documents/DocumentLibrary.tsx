import { useConfirmation } from "@/components/ui/use-confirmation";
import { useRef, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { errorMessage } from "@/features/auth/auth-api";
import {
  downloadDocument,
  useReplaceDocument,
  useUpdateDocument,
  type ClubDocument,
  type RoleOption,
} from "./document-hooks";

function size(value: number) {
  return value < 1024
    ? `${value} B`
    : value < 1024 * 1024
      ? `${(value / 1024).toFixed(1)} KB`
      : `${(value / 1024 / 1024).toFixed(1)} MB`;
}
function DocumentCard({
  document,
  roles,
  canManage,
}: {
  document: ClubDocument;
  roles: RoleOption[];
  canManage: boolean;
}) {
  const { confirm, confirmation } = useConfirmation();
  const replacementInput = useRef<HTMLInputElement>(null);
  const [validation, setValidation] = useState("");
  const update = useUpdateDocument();
  const replace = useReplaceDocument();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(document.title);
  const [category, setCategory] = useState(document.category);
  const [selected, setSelected] = useState(document.visibleRoleCodes);
  const [replacement, setReplacement] = useState<File | null>(null);
  const [downloadError, setDownloadError] = useState("");
  const save = (event: FormEvent) => {
    event.preventDefault();
    setValidation("");
    if (selected.length === 0) return;
    if (!title.trim() || !category.trim()) {
      setValidation("Enter a title and category.");
      return;
    }
    confirm(
      "Save document access",
      `Save “${title.trim()}” with access for ${roles
        .filter((role) => selected.includes(role.code))
        .map((role) => role.name)
        .join(", ")}? Members outside these roles will not be able to read it.`,
      () =>
        update.mutate(
          {
            id: document.id,
            metadata: {
              version: document.version,
              title: title.trim(),
              category: category.trim(),
              visibleRoleCodes: selected,
            },
          },
          { onSuccess: () => setEditing(false) },
        ),
    );
  };
  return (
    <article className="space-y-3 rounded-2xl border border-border/75 bg-card p-4 shadow-[0_1px_2px_oklch(0.2_0.03_160/4%)]">
      {confirmation}
      {validation && <p role="alert">{validation}</p>}
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 className="font-semibold">{document.title}</h3>
          <p className="text-sm text-muted-foreground">
            {document.category} · updated{" "}
            {new Date(document.updatedAt).toLocaleString()}
          </p>
        </div>
        {canManage && !editing && (
          <Button
            type="button"
            variant="outline"
            onClick={() => setEditing(true)}
          >
            Edit access
          </Button>
        )}
      </div>
      {editing && (
        <form className="space-y-2" onSubmit={save}>
          <label>
            Title
            <Input
              maxLength={200}
              value={title}
              required
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>
          <label>
            Category
            <Input
              maxLength={80}
              value={category}
              required
              onChange={(event) => setCategory(event.target.value)}
            />
          </label>
          <fieldset>
            <legend>Visible to roles</legend>
            <div className="flex flex-wrap gap-3">
              {roles.map((role) => (
                <label className="flex gap-2" key={role.code}>
                  <input
                    type="checkbox"
                    checked={selected.includes(role.code)}
                    onChange={(event) =>
                      setSelected((current) =>
                        event.target.checked
                          ? [...current, role.code]
                          : current.filter((code) => code !== role.code),
                      )
                    }
                  />
                  {role.name}
                </label>
              ))}
            </div>
          </fieldset>
          {selected.length === 0 && (
            <p role="alert">Select at least one role.</p>
          )}
          {update.error && <p role="alert">{errorMessage(update.error)}</p>}
          <div className="flex gap-2">
            <Button
              type="submit"
              disabled={update.isPending || selected.length === 0}
            >
              Save
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => setEditing(false)}
            >
              Cancel
            </Button>
          </div>
        </form>
      )}
      <div>
        <h4 className="font-medium">Version history</h4>
        <ol className="space-y-2">
          {document.versions.map((version) => (
            <li
              className="flex flex-wrap items-center justify-between gap-2 border-t pt-2"
              key={version.id}
            >
              <span>
                v{version.versionNumber} · {version.fileName} ·{" "}
                {size(version.fileSize)} ·{" "}
                {new Date(version.uploadedAt).toLocaleString()}
              </span>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setDownloadError("");
                  void downloadDocument(document, version).catch((error) =>
                    setDownloadError(errorMessage(error)),
                  );
                }}
              >
                Download v{version.versionNumber}
              </Button>
            </li>
          ))}
        </ol>
      </div>
      {downloadError && <p role="alert">{downloadError}</p>}
      {canManage && (
        <div className="flex flex-wrap items-end gap-2">
          <label>
            Upload a new version
            <Input
              ref={replacementInput}
              type="file"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.txt,.png,.jpg,.jpeg"
              onChange={(event) =>
                setReplacement(event.target.files?.[0] ?? null)
              }
            />
          </label>
          <Button
            type="button"
            disabled={!replacement || replace.isPending}
            onClick={() => {
              setValidation("");
              if (!replacement) return;
              if (
                replacement.size === 0 ||
                replacement.size > 5 * 1024 * 1024
              ) {
                setValidation(
                  "Documents must be non-empty and 5 MB or smaller.",
                );
                return;
              }
              confirm(
                "Add document version",
                `Add ${replacement.name} as the latest version of “${document.title}”? Existing versions remain in the history.`,
                () =>
                  replace.mutate(
                    {
                      id: document.id,
                      version: document.version,
                      file: replacement,
                    },
                    {
                      onSuccess: () => {
                        setReplacement(null);
                        if (replacementInput.current)
                          replacementInput.current.value = "";
                      },
                    },
                  ),
              );
            }}
          >
            {replace.isPending ? "Uploading…" : "Add version"}
          </Button>
        </div>
      )}
      {replace.error && <p role="alert">{errorMessage(replace.error)}</p>}
    </article>
  );
}
export function DocumentLibrary({
  documents,
  roles,
  canManage,
}: {
  documents: ClubDocument[];
  roles: RoleOption[];
  canManage: boolean;
}) {
  return (
    <section className="space-y-4">
      <h2 className="font-heading text-xl font-semibold tracking-tight">Document library</h2>
      {documents.length === 0 ? (
        <p>No documents are visible to you yet.</p>
      ) : (
        documents.map((document) => (
          <DocumentCard
            key={document.id}
            document={document}
            roles={roles}
            canManage={canManage}
          />
        ))
      )}
    </section>
  );
}
