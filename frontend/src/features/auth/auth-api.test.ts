import axios, { AxiosHeaders, type AxiosResponse } from "axios"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { api, errorMessage, refreshSession, type AuthResponse } from "./auth-api"
import { useAuthStore, type ClubSummary } from "./auth-store"

const originalAdapter = api.defaults.adapter
const club: ClubSummary = { id: "club-a", name: "Savings", clubType: "INVESTMENT_CLUB", administrator: true }
const user = { id: "user-a", email: "test@example.com" }

describe("tenant session transport", () => {
  beforeEach(() => { useAuthStore.getState().clearSession(); useAuthStore.getState().setSession("token", user, club) })
  afterEach(() => { api.defaults.adapter = originalAdapter; useAuthStore.getState().clearSession() })

  it("discards an old club response after switching starts", async () => {
    let complete!: (response: AxiosResponse) => void
    let started!: () => void
    const ready = new Promise<void>((resolve) => { started = resolve })
    api.defaults.adapter = (config) => new Promise((resolve) => {
      complete = (response) => resolve({ ...response, config }); started()
    })
    const request = api.get("/club").catch((error: unknown) => error)
    await ready
    useAuthStore.getState().beginClubSwitch()
    complete({ data: club, status: 200, statusText: "OK", headers: {}, config: { headers: new AxiosHeaders() } })
    expect(axios.isCancel(await request)).toBe(true)
  })

  it("blocks tenant requests during a club switch", async () => {
    useAuthStore.getState().beginClubSwitch()
    const error = await api.get("/club").catch((failure: unknown) => failure)
    expect(axios.isCancel(error)).toBe(true)
  })

  it("restores the active club and shares simultaneous refreshes", async () => {
    let calls = 0
    const session: AuthResponse = { accessToken: "renewed", expiresInSeconds: 900, user, activeClub: club }
    api.defaults.adapter = async (config) => {
      calls++
      expect(config.headers.Authorization).toBeUndefined()
      return { data: session, status: 200, statusText: "OK", headers: {}, config }
    }
    await Promise.all([refreshSession(), refreshSession()])
    expect(calls).toBe(1)
    expect(useAuthStore.getState().activeClub).toEqual(club)
    expect(useAuthStore.getState().accessToken).toBe("renewed")
  })
})

describe("user-facing API errors", () => {
  const failure = (status: number, detail?: string) => new axios.AxiosError("Request failed", undefined, undefined, undefined, {
    data: detail ? { detail } : {}, status, statusText: "Error", headers: {}, config: { headers: new AxiosHeaders() },
  })

  it("keeps concise client-safe problem details", () => {
    expect(errorMessage(failure(400, "A contribution amount must be positive."))).toBe("A contribution amount must be positive.")
  })

  it("replaces server failures and stack traces with actionable messages", () => {
    expect(errorMessage(failure(500, "java.lang.IllegalStateException: secret\n at service.save(Service.java:42)"))).toBe("The service is temporarily unavailable. Please try again shortly.")
    expect(errorMessage(failure(403))).toBe("You do not have permission to perform this action.")
  })

  it("uses the supplied context when the failure is unknown", () => {
    expect(errorMessage(new Error("internal details"), "We couldn't save the meeting. Try again.")).toBe("We couldn't save the meeting. Try again.")
  })
})
