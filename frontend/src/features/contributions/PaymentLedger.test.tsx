import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, expect, it, vi } from "vitest";
import { useAuthStore } from "@/features/auth/auth-store";
import { MyLedger } from "./MyLedger";
import { RecordPayment } from "./RecordPayment";
const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }));
vi.mock("@/features/auth/auth-api", () => ({
  api: { get, post },
  errorMessage: () => "Payment failed",
}));
const expectation = {
  scheduleVersionId: "version-1",
  scheduleName: "Monthly savings",
  membershipId: "member-1",
  memberName: "Member One",
  dueDate: "2026-09-01",
  expected: 100,
  paid: 40,
  outstanding: 60,
  currency: "ZAR",
};
const ledger = {
  membershipId: "member-1",
  from: "2026-01-01",
  to: "2026-12-31",
  totalExpected: 100,
  totalPaid: 40,
  balance: 60,
  currency: "ZAR",
  lines: [
    {
      type: "EXPECTED",
      activityDate: "2026-09-01",
      description: "Monthly savings",
      scheduleVersionId: "version-1",
      paymentId: null,
      expected: 100,
      paid: 0,
      runningBalance: 100,
      currency: "ZAR",
    },
    {
      type: "PAYMENT",
      activityDate: "2026-09-02",
      description: "Payment received",
      scheduleVersionId: "version-1",
      paymentId: "payment-1",
      expected: 0,
      paid: 40,
      runningBalance: 60,
      currency: "ZAR",
    },
  ],
};
beforeEach(() => {
  vi.clearAllMocks();
  useAuthStore
    .getState()
    .setSession(
      "token",
      { id: "member-user", email: "member@example.test" },
      {
        id: "club-1",
        name: "Club",
        clubType: "INVESTMENT_CLUB",
        administrator: false,
        permissions: ["CONTRIBUTIONS_READ", "CONTRIBUTIONS_WRITE"],
      },
    );
  get.mockImplementation((path: string) =>
    Promise.resolve({
      data: path.endsWith("my-ledger") ? ledger : [expectation],
    }),
  );
  post.mockResolvedValue({ data: {} });
});
function renderWithQuery(component: React.ReactNode) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>{component}</QueryClientProvider>,
  );
}
it("renders the authenticated member ledger and running balance", async () => {
  renderWithQuery(<MyLedger />);
  expect((await screen.findAllByText("R 60.00")).length).toBeGreaterThan(0);
  expect(screen.getByText("Monthly savings")).toBeInTheDocument();
  expect(
    screen.getByText("Only your authenticated membership can be shown here."),
  ).toBeInTheDocument();
});
it("records payments and sends targeted reminders", async () => {
  renderWithQuery(<RecordPayment />);
  expect(
    await screen.findByRole("option", { name: /Member One/ }),
  ).toBeInTheDocument();
  fireEvent.click(
    screen.getByRole("button", { name: "Mark payment received" }),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Confirm" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith(
      "/contribution-payments",
      expect.any(FormData),
    ),
  );
  fireEvent.click(screen.getByRole("button", { name: "Send reminder" }));
  fireEvent.click(await screen.findByRole("button", { name: "Confirm" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith(
      "/contribution-payments/reminders",
      expect.objectContaining({ membershipId: "member-1" }),
    ),
  );
});

it("reviews payment details and makes no request when cancelled", async () => {
  renderWithQuery(<RecordPayment />);
  await screen.findByRole("option", { name: /Member One/ });
  fireEvent.click(
    screen.getByRole("button", { name: "Mark payment received" }),
  );
  expect(await screen.findByRole("dialog")).toHaveTextContent(
    "R 60.00 for Member One",
  );
  expect(post).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Go back" }));
  expect(post).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Amount (ZAR)")).toHaveValue(60);
});

it("blocks oversized proof before review or recording", async () => {
  renderWithQuery(<RecordPayment />);
  await screen.findByRole("option", { name: /Member One/ });
  const file = new File([new Uint8Array(1024 * 1024 + 1)], "proof.pdf", {
    type: "application/pdf",
  });
  fireEvent.change(screen.getByLabelText("Proof (optional, maximum 1 MB)"), {
    target: { files: [file] },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Mark payment received" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "no larger than 1 MB",
  );
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(post).not.toHaveBeenCalled();
});
