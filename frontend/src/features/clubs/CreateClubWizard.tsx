import { useState } from "react"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useCreateClub } from "./club-hooks"
import type { ClubSummary } from "@/features/auth/auth-store"

export function CreateClubWizard({ onCreated }: { onCreated: (club: ClubSummary) => void }) {
  const [reviewing, setReviewing] = useState(false)
  const createClub = useCreateClub()
  const { register, handleSubmit, getValues, formState: { errors } } = useForm<{ name: string }>({ defaultValues: { name: "" } })

  return (
    <section className="rounded-2xl border bg-card p-6">
      <p className="text-sm text-muted-foreground">Step {reviewing ? "2" : "1"} of 2</p>
      <h2 className="mt-1 text-xl font-semibold">Create a club</h2>
      <form className="mt-5 space-y-4" onSubmit={handleSubmit(({ name }) => {
        if (!reviewing) { setReviewing(true); return }
        createClub.mutate(name.trim(), { onSuccess: (club) => onCreated(club) })
      })}>
        {!reviewing ? <>
          <label className="block text-sm font-medium" htmlFor="club-name">Club name</label>
          <Input id="club-name" maxLength={120} aria-invalid={Boolean(errors.name)} {...register("name", { validate: (value) => value.trim().length > 0 || "Enter a club name", maxLength: 120 })} />
          {errors.name && <p role="alert" className="text-sm text-destructive">{errors.name.message}</p>}
          <label className="block text-sm font-medium" htmlFor="club-type">Club type</label>
          <select id="club-type" className="w-full rounded-lg border p-2" value="INVESTMENT_CLUB" disabled><option value="INVESTMENT_CLUB">Investment Club</option></select>
          <p className="text-sm text-muted-foreground">More club types will be added later.</p>
        </> : <div className="rounded-lg bg-muted p-4">
          <p className="font-medium">{getValues("name").trim()}</p>
          <p className="text-sm">Investment Club</p>
          <p className="mt-2 text-sm">You will be this club’s administrator and can assign roles to its members.</p>
        </div>}
        {createClub.error && <p role="alert" className="text-sm text-destructive">{errorMessage(createClub.error)}</p>}
        <div className="flex gap-3">
          {reviewing && <Button type="button" variant="outline" disabled={createClub.isPending || createClub.isSuccess} onClick={() => setReviewing(false)}>Back</Button>}
          <Button type="submit" disabled={createClub.isPending || createClub.isSuccess}>{createClub.isSuccess ? "Club created" : createClub.isPending ? "Creating…" : reviewing ? "Create club" : "Review club"}</Button>
        </div>
      </form>
    </section>
  )
}
