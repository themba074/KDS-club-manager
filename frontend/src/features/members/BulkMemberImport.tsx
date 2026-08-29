import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { errorMessage } from "@/features/auth/auth-api"
import {
  useConfirmMemberImport,
  useInspectMemberImport,
  usePreviewMemberImport,
  type ImportColumnMapping,
  type ImportRow,
} from "./member-hooks"

const emptyMapping: ImportColumnMapping = { emailColumn: "", firstNameColumn: "", lastNameColumn: "" }

function guess(headers: string[], terms: string[]) {
  return headers.find((header) => terms.includes(header.toLowerCase().replace(/[_ -]/g, ""))) ?? ""
}

export function BulkMemberImport() {
  const [file, setFile] = useState<File | null>(null)
  const [mapping, setMapping] = useState<ImportColumnMapping>(emptyMapping)
  const inspection = useInspectMemberImport()
  const preview = usePreviewMemberImport()
  const confirmation = useConfirmMemberImport()

  const inspect = () => {
    if (!file) return
    preview.reset()
    confirmation.reset()
    inspection.mutate(file, { onSuccess: ({ headers }) => setMapping({
      emailColumn: guess(headers, ["email", "emailaddress"]),
      firstNameColumn: guess(headers, ["first", "firstname", "givenname"]),
      lastNameColumn: guess(headers, ["last", "lastname", "surname", "familyname"]),
      phoneColumn: guess(headers, ["phone", "phonenumber", "mobile"]) || undefined,
    }) })
  }
  const canPreview = file && mapping.emailColumn && mapping.firstNameColumn && mapping.lastNameColumn
  const rows = confirmation.data?.rows ?? preview.data?.rows

  return <section className="rounded-xl border bg-card p-5">
    <h2 className="text-lg font-semibold">Bulk import members</h2>
    <p className="mt-1 text-sm text-muted-foreground">Upload a CSV, map its columns, and review every row. Valid rows create invitations; invalid rows are left unchanged.</p>
    <div className="mt-4 flex flex-wrap items-end gap-3">
      <label className="min-w-64 flex-1 text-sm font-medium">CSV file
        <Input type="file" accept=".csv,text/csv" onChange={(event) => {
          setFile(event.target.files?.[0] ?? null)
          setMapping(emptyMapping)
          inspection.reset(); preview.reset(); confirmation.reset()
        }} />
      </label>
      <Button type="button" variant="outline" disabled={!file || inspection.isPending} onClick={inspect}>
        {inspection.isPending ? "Reading…" : "Read columns"}
      </Button>
    </div>
    {inspection.error && <p role="alert" className="mt-3 text-sm text-destructive">{errorMessage(inspection.error)}</p>}
    {inspection.data && <>
      <p className="mt-4 text-sm">Found {inspection.data.rowCount} data row{inspection.data.rowCount === 1 ? "" : "s"}. Match the required fields below.</p>
      <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <ColumnSelect label="Email" value={mapping.emailColumn} headers={inspection.data.headers} onChange={(value) => setMapping({ ...mapping, emailColumn: value })} />
        <ColumnSelect label="First name" value={mapping.firstNameColumn} headers={inspection.data.headers} onChange={(value) => setMapping({ ...mapping, firstNameColumn: value })} />
        <ColumnSelect label="Last name" value={mapping.lastNameColumn} headers={inspection.data.headers} onChange={(value) => setMapping({ ...mapping, lastNameColumn: value })} />
        <ColumnSelect label="Phone (optional)" value={mapping.phoneColumn ?? ""} headers={inspection.data.headers} optional onChange={(value) => setMapping({ ...mapping, phoneColumn: value || undefined })} />
      </div>
      <Button className="mt-4" type="button" disabled={!canPreview || preview.isPending} onClick={() => file && preview.mutate({ file, mapping }, { onSuccess: () => confirmation.reset() })}>
        {preview.isPending ? "Checking rows…" : "Preview import"}
      </Button>
    </>}
    {preview.error && <p role="alert" className="mt-3 text-sm text-destructive">{errorMessage(preview.error)}</p>}
    {preview.data && !confirmation.data && <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg bg-muted p-3 text-sm">
      <span>{preview.data.readyRows} ready · {preview.data.invalidRows} invalid</span>
      <Button type="button" disabled={preview.data.readyRows === 0 || confirmation.isPending} onClick={() => file && confirmation.mutate({ file, mapping })}>
        {confirmation.isPending ? "Creating invitations…" : `Import ${preview.data.readyRows} valid row${preview.data.readyRows === 1 ? "" : "s"}`}
      </Button>
    </div>}
    {confirmation.error && <p role="alert" className="mt-3 text-sm text-destructive">{errorMessage(confirmation.error)}</p>}
    {confirmation.data && <p role="status" className="mt-4 rounded-lg bg-primary/10 p-3 text-sm">
      Created {confirmation.data.invitedRows} invitation{confirmation.data.invitedRows === 1 ? "" : "s"}; {confirmation.data.failedRows} row{confirmation.data.failedRows === 1 ? "" : "s"} were not imported.
    </p>}
    {rows && <ImportRows rows={rows} />}
  </section>
}

function ColumnSelect({ label, value, headers, optional = false, onChange }: {
  label: string; value: string; headers: string[]; optional?: boolean; onChange: (value: string) => void
}) {
  return <label className="text-sm font-medium">{label}
    <select className="mt-1 h-9 w-full rounded-lg border bg-background px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">{optional ? "Not included" : "Choose a column"}</option>
      {headers.map((header) => <option key={header} value={header}>{header}</option>)}
    </select>
  </label>
}

function ImportRows({ rows }: { rows: ImportRow[] }) {
  return <div className="mt-4 max-h-96 overflow-auto rounded-xl border">
    <Table><TableHeader><TableRow><TableHead>Row</TableHead><TableHead>Name</TableHead><TableHead>Email</TableHead><TableHead>Result</TableHead></TableRow></TableHeader>
      <TableBody>{rows.map((row) => <TableRow key={row.rowNumber}>
        <TableCell>{row.rowNumber}</TableCell><TableCell>{`${row.firstName} ${row.lastName}`.trim() || "—"}</TableCell><TableCell>{row.email || "—"}</TableCell>
        <TableCell><span className={row.status === "READY" || row.status === "INVITED" ? "text-primary" : "text-destructive"}>
          {row.status === "READY" ? "Ready" : row.status === "INVITED" ? "Invited" : row.errors.join(" ")}
        </span></TableCell>
      </TableRow>)}</TableBody>
    </Table>
  </div>
}
