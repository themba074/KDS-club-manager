import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useClubTypes, useCreateClub } from "./club-hooks"
import type { ClubSummary } from "@/features/auth/auth-store"

export function CreateClubWizard({ onCreated }: { onCreated: (club: ClubSummary) => void }) {
  const [reviewing, setReviewing] = useState(false)
  const createClub = useCreateClub()
  const clubTypes = useClubTypes()
  const { register, handleSubmit, getValues, setValue, formState: { errors } } = useForm<{ name: string; clubType: string }>({ defaultValues: { name: "", clubType: "" } })
  useEffect(() => {
    if (clubTypes.data?.length && !clubTypes.data.some((type) => type.code === getValues("clubType"))) {
      setValue("clubType", clubTypes.data[0].code)
    }
  }, [clubTypes.data, getValues, setValue])
  const selectedType = clubTypes.data?.find((type) => type.code === getValues("clubType"))

  return (
    <section className="rounded-2xl border bg-card p-6">
      <p className="text-sm text-muted-foreground">Step {reviewing ? "2" : "1"} of 2</p>
      <h2 className="mt-1 text-xl font-semibold">Create a club</h2>
      <form className="mt-5 space-y-4" onSubmit={handleSubmit(({ name, clubType }) => {
        if (!reviewing) { setReviewing(true); return }
        createClub.mutate({ name: name.trim(), clubType }, { onSuccess: (club) => onCreated(club) })
      })}>
        {!reviewing ? <>
          <label className="block text-sm font-medium" htmlFor="club-name">Club name</label>
          <Input id="club-name" maxLength={120} aria-invalid={Boolean(errors.name)} {...register("name", { validate: (value) => value.trim().length > 0 || "Enter a club name", maxLength: 120 })} />
          {errors.name && <p role="alert" className="text-sm text-destructive">{errors.name.message}</p>}
          <label className="block text-sm font-medium" htmlFor="club-type">Club type</label>
          <select id="club-type" className="w-full rounded-lg border p-2" {...register("clubType", { required: true })}>
            {clubTypes.data?.map((type) => <option key={type.code} value={type.code}>{type.name}</option>)}
          </select>
          {clubTypes.isPending && <p className="text-sm text-muted-foreground">Loading club types…</p>}
          {clubTypes.isError && <p role="alert" className="text-sm text-destructive">Could not load club types.</p>}
        </> : <div className="rounded-lg bg-muted p-4">
          <p className="font-medium">{getValues("name").trim()}</p>
          <p className="text-sm">{selectedType?.name ?? getValues("clubType")}</p>
          <p className="mt-2 text-sm">You will be this club’s administrator and can assign roles to its members.</p>
        </div>}
        {createClub.error && <p role="alert" className="text-sm text-destructive">{errorMessage(createClub.error)}</p>}
        <div className="flex gap-3">
          {reviewing && <Button type="button" variant="outline" disabled={createClub.isPending || createClub.isSuccess} onClick={() => setReviewing(false)}>Back</Button>}
          <Button type="submit" disabled={createClub.isPending || createClub.isSuccess || !clubTypes.data?.length}>{createClub.isSuccess ? "Club created" : createClub.isPending ? "Creating…" : reviewing ? "Create club" : "Review club"}</Button>
        </div>
      </form>
    </section>
  )
}
