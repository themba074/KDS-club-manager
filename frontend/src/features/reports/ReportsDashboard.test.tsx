import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { beforeEach, expect, it, vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { ReportsDashboard } from "./ReportsDashboard"

const { get } = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock("@/features/auth/auth-api", () => ({ api: { get }, errorMessage: () => "Export failed" }))
vi.mock("@/features/contributions/ReportExport", () => ({ ReportExport: () => <div>Contribution report</div> }))

beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.getState().setSession("token", { id: "owner", email: "owner@example.test" }, {
    id: "club-1", name: "Club", clubType: "INVESTMENT_CLUB", administrator: true,
    permissions: ["REPORTS_READ", "MEMBERS_READ", "MEETINGS_READ"],
  })
  Object.defineProperty(URL, "createObjectURL", { value: vi.fn(() => "blob:report"), configurable: true })
  Object.defineProperty(URL, "revokeObjectURL", { value: vi.fn(), configurable: true })
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined)
  get.mockResolvedValue({ data: new Blob(["report"]) })
})

function page() {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><ReportsDashboard /></QueryClientProvider>)
}

it("only shows exports for modules the user can read", () => {
  page()
  expect(screen.getByText("Member list")).toBeInTheDocument()
  expect(screen.getByText("Meeting history")).toBeInTheDocument()
  expect(screen.queryByText("Voting history")).not.toBeInTheDocument()
  expect(screen.getByText("Contribution report")).toBeInTheDocument()
})

it("downloads the selected report through the shared export endpoint", async () => {
  page()
  const card = screen.getByText("Member list").closest("[data-slot='card']")!
  fireEvent.click(card.querySelector("button")!)
  await waitFor(() => expect(get).toHaveBeenCalledWith("/reports/members/export", expect.objectContaining({
    params: expect.objectContaining({ format: "CSV" }), responseType: "blob",
  })))
  expect(URL.createObjectURL).toHaveBeenCalled()
})
