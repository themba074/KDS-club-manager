import { beforeEach, describe, expect, it } from "vitest"
import { useAuthStore } from "./auth-store"

describe("auth store", () => {
  beforeEach(() => useAuthStore.setState({ accessToken: null, user: null, initialized: false }))

  it("keeps and clears an in-memory session", () => {
    useAuthStore.getState().setSession("access-token", { id: "user-1", email: "member@example.com" })
    expect(useAuthStore.getState().accessToken).toBe("access-token")
    useAuthStore.getState().clearSession()
    expect(useAuthStore.getState()).toMatchObject({ accessToken: null, user: null, initialized: true })
  })
})
