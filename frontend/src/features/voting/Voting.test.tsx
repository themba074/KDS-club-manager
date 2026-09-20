import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { useAuthStore } from "@/features/auth/auth-store";
import { VotingPage } from "./index";
import type { Motion, MotionResult } from "./motion-hooks";

const { get, post, put } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));
vi.mock("@/features/auth/auth-api", () => ({
  api: { get, post, put },
  errorMessage: () => "Motion request failed",
}));
let motions: Motion[];
let results: MotionResult;
let client: QueryClient;
const member = {
  id: "member-1",
  email: "member@example.test",
  firstName: "Lindiwe",
  lastName: "Dube",
  status: "ACTIVE",
};
function draft(): Motion {
  return {
    id: "motion-1",
    version: 3,
    title: "Approve budget",
    description: "Budget details",
    opensAt: new Date(Date.now() + 3_600_000).toISOString(),
    closesAt: new Date(Date.now() + 86_400_000).toISOString(),
    state: "DRAFT",
    options: [
      { id: "option-1", position: 0, label: "Yes" },
      { id: "option-2", position: 1, label: "No" },
    ],
    eligibleVoterCount: 1,
    eligibleToVote: true,
    eligibleMembershipIds: [member.id],
    selectedOptionId: null,
    resultsPublished: false,
  };
}
beforeEach(() => {
  vi.resetAllMocks();
  useAuthStore.getState().setSession(
    "token",
    { id: "owner", email: "owner@example.test" },
    {
      id: "club-1",
      name: "Club",
      clubType: "INVESTMENT_CLUB",
      administrator: true,
      permissions: ["VOTES_READ", "VOTES_CREATE", "VOTES_CAST", "MEMBERS_READ"],
    },
  );
  motions = [draft()];
  results = {
    motionId: "motion-1",
    published: false,
    publishedAt: null,
    totalVotes: 1,
    eligibleVoterCount: 3,
    outcome: "NO_MAJORITY",
    winningOptionId: null,
    options: [
      { optionId: "option-1", position: 0, label: "Yes", voteCount: 1 },
      { optionId: "option-2", position: 1, label: "No", voteCount: 0 },
    ],
  };
  get.mockImplementation((path: string) => {
    if (path === "/motions") return Promise.resolve({ data: motions });
    if (path === "/members") return Promise.resolve({ data: [member] });
    return Promise.resolve({ data: results });
  });
  post.mockImplementation((path: string, body: { optionId?: string }) => {
    if (path.endsWith("/cancel"))
      motions = motions.map((motion) => ({
        ...motion,
        state: "CANCELLED",
        version: motion.version + 1,
      }));
    if (path.endsWith("/votes")) {
      return Promise.resolve({
        data: {
          motionId: "motion-1",
          optionId: body.optionId,
          castAt: new Date().toISOString(),
        },
      });
    }
    if (path.endsWith("/results/publish")) {
      results = {
        ...results,
        published: true,
        publishedAt: new Date().toISOString(),
      };
      motions = motions.map((motion) => ({
        ...motion,
        resultsPublished: true,
        version: motion.version + 1,
      }));
      return Promise.resolve({ data: results });
    }
    return Promise.resolve({ data: motions[0] });
  });
  put.mockResolvedValue({ data: { ...draft(), version: 4 } });
});
afterEach(() => {
  client?.clear();
  vi.useRealTimers();
});
function page() {
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <VotingPage />
    </QueryClientProvider>,
  );
}
function fillCreate() {
  fireEvent.change(screen.getByLabelText("Title"), {
    target: { value: "New motion" },
  });
  fireEvent.change(screen.getByLabelText("Option 1"), {
    target: { value: "Approve" },
  });
  fireEvent.change(screen.getByLabelText("Option 2"), {
    target: { value: "Reject" },
  });
}

it("shows motions to read-only members and never offers voting to an ineligible member", async () => {
  motions = [
    {
      ...draft(),
      state: "OPEN",
      eligibleToVote: false,
      eligibleMembershipIds: [],
    },
  ];
  useAuthStore.setState((state) => ({
    activeClub: {
      ...state.activeClub!,
      permissions: ["VOTES_READ", "VOTES_CAST"],
    },
  }));
  page();
  expect(await screen.findByText("Approve budget")).toBeInTheDocument();
  expect(
    screen.getByText("You are not eligible to vote on this motion."),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Create motion" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Edit motion" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Cancel motion" }),
  ).not.toBeInTheDocument();
  expect(get).not.toHaveBeenCalledWith("/members", expect.anything());
});

it("requires a choice, casts one ballot, and replaces the ballot with a receipt", async () => {
  motions = [{ ...draft(), state: "OPEN" }];
  page();
  await screen.findByText("Approve budget");
  fireEvent.click(screen.getByRole("button", { name: "Cast vote" }));
  if (screen.queryByRole("dialog"))
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Choose an option before casting your vote.",
  );
  expect(post).not.toHaveBeenCalledWith(
    "/motions/motion-1/votes",
    expect.anything(),
  );

  fireEvent.click(screen.getByLabelText("Yes"));
  fireEvent.click(screen.getByRole("button", { name: "Cast vote" }));
  if (screen.queryByRole("dialog"))
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith("/motions/motion-1/votes", {
      optionId: "option-1",
    }),
  );
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Vote recorded for Yes. Votes cannot be changed.",
  );
  expect(
    screen.queryByRole("button", { name: "Cast vote" }),
  ).not.toBeInTheDocument();
});

it("keeps the ballot selected when vote submission fails", async () => {
  motions = [{ ...draft(), state: "OPEN" }];
  post.mockRejectedValueOnce(new Error("offline"));
  page();
  await screen.findByText("Approve budget");
  fireEvent.click(screen.getByLabelText("No"));
  fireEvent.click(screen.getByRole("button", { name: "Cast vote" }));
  if (screen.queryByRole("dialog"))
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Motion request failed",
  );
  expect(screen.getByLabelText("No")).toBeChecked();
});

it("lets a manager review a closed tally and publish its immutable snapshot", async () => {
  motions = [{ ...draft(), state: "CLOSED" }];
  page();
  expect(await screen.findByText("Result preview")).toBeInTheDocument();
  expect(
    screen.getByText("1 of 3 eligible members voted."),
  ).toBeInTheDocument();
  expect(
    screen.getByText("No option received a simple majority."),
  ).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Publish results" }));
  if (screen.queryByRole("dialog"))
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith("/motions/motion-1/results/publish", {
      version: 3,
    }),
  );
  expect(await screen.findByText("Published results")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Publish results" }),
  ).not.toBeInTheDocument();
});

it("keeps an unpublished tally private from ordinary members", async () => {
  motions = [{ ...draft(), state: "CLOSED", eligibleToVote: false }];
  useAuthStore.setState((state) => ({
    activeClub: {
      ...state.activeClub!,
      administrator: false,
      permissions: ["VOTES_READ"],
    },
  }));
  page();
  expect(
    await screen.findByText("Results will be available after publication."),
  ).toBeInTheDocument();
  expect(get).not.toHaveBeenCalledWith(
    "/motions/motion-1/results",
    expect.anything(),
  );
});

it("shows a published result and its winner to ordinary members", async () => {
  motions = [
    {
      ...draft(),
      state: "CLOSED",
      eligibleToVote: false,
      resultsPublished: true,
    },
  ];
  results = {
    ...results,
    published: true,
    publishedAt: "2026-09-12T10:00:00Z",
    outcome: "WINNER",
    winningOptionId: "option-1",
  };
  useAuthStore.setState((state) => ({
    activeClub: {
      ...state.activeClub!,
      administrator: false,
      permissions: ["VOTES_READ"],
    },
  }));
  page();
  expect(await screen.findByText("Published results")).toBeInTheDocument();
  expect(screen.getByText("Winner: Yes")).toBeInTheDocument();
});

it("creates a motion with ordered options, refreshes the list, and clears the form", async () => {
  page();
  await screen.findByText("Approve budget");
  fillCreate();
  fireEvent.click(screen.getByRole("button", { name: "Add option" }));
  fireEvent.change(screen.getByLabelText("Option 3"), {
    target: { value: "Abstain" },
  });
  const opening = (screen.getByLabelText("Opens at") as HTMLInputElement).value;
  fireEvent.click(screen.getByRole("button", { name: "Create motion" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith(
      "/motions",
      expect.objectContaining({
        title: "New motion",
        options: ["Approve", "Reject", "Abstain"],
        allActiveMembers: true,
        opensAt: new Date(opening).toISOString(),
        eligibleMembershipIds: [],
      }),
    ),
  );
  expect(await screen.findByText("Motion created.")).toBeInTheDocument();
  expect(screen.getByLabelText("Title")).toHaveValue("");
  expect(
    get.mock.calls.filter(([path]) => path === "/motions").length,
  ).toBeGreaterThan(1);
});

it("submits a selected voter set and prevents an empty selection", async () => {
  page();
  await screen.findByText("Approve budget");
  fillCreate();
  fireEvent.click(screen.getByLabelText("All active members may vote"));
  fireEvent.click(screen.getByRole("button", { name: "Create motion" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Select at least one eligible member.",
  );
  expect(post).not.toHaveBeenCalled();
  fireEvent.click(screen.getByLabelText("Lindiwe Dube"));
  fireEvent.click(screen.getByRole("button", { name: "Create motion" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith(
      "/motions",
      expect.objectContaining({
        eligibleMembershipIds: [member.id],
        allActiveMembers: false,
      }),
    ),
  );
});

it("edits a draft with its original version and preserves the voter snapshot", async () => {
  page();
  fireEvent.click(await screen.findByRole("button", { name: "Edit motion" }));
  const form = within(
    screen.getByRole("heading", { name: "Edit motion" }).closest("form")!,
  );
  expect(form.getByLabelText("All active members may vote")).not.toBeChecked();
  expect(form.getByLabelText("Lindiwe Dube")).toBeChecked();
  fireEvent.change(form.getByLabelText("Title"), {
    target: { value: "Corrected budget" },
  });
  fireEvent.click(form.getByRole("button", { name: "Save motion" }));
  await waitFor(() =>
    expect(put).toHaveBeenCalledWith(
      "/motions/motion-1",
      expect.objectContaining({
        version: 3,
        title: "Corrected budget",
        allActiveMembers: false,
        eligibleMembershipIds: [member.id],
        options: ["Yes", "No"],
      }),
    ),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("heading", { name: "Edit motion" }),
    ).not.toBeInTheDocument(),
  );
});

it("hides edit controls for open and closed motions but permits cancelled corrections", async () => {
  motions = [
    { ...draft(), state: "OPEN" },
    { ...draft(), id: "closed", title: "Closed budget", state: "CLOSED" },
    {
      ...draft(),
      id: "cancelled",
      title: "Cancelled budget",
      state: "CANCELLED",
    },
  ];
  page();
  await screen.findByText("Cancelled budget");
  expect(screen.getAllByRole("button", { name: "Edit motion" })).toHaveLength(
    1,
  );
  fireEvent.click(screen.getByRole("button", { name: "Edit motion" }));
  expect(
    screen.getByText("This motion will remain cancelled after saving."),
  ).toBeInTheDocument();
});

it("shows cancellation failures and refreshes after successful cancellation", async () => {
  post.mockRejectedValueOnce(new Error("conflict"));
  page();
  fireEvent.click(await screen.findByRole("button", { name: "Cancel motion" }));
  if (screen.queryByRole("dialog"))
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Motion request failed",
  );
  expect(post).toHaveBeenCalledWith("/motions/motion-1/cancel", { version: 3 });
  fireEvent.click(screen.getByRole("button", { name: "Cancel motion" }));
  if (screen.queryByRole("dialog"))
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  expect(await screen.findByText("CANCELLED")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Cancel motion" }),
  ).not.toBeInTheDocument();
});

it("keeps entered data on a save failure", async () => {
  post.mockRejectedValue(new Error("offline"));
  page();
  await screen.findByText("Approve budget");
  fillCreate();
  fireEvent.click(screen.getByRole("button", { name: "Create motion" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Motion request failed",
  );
  expect(screen.getByLabelText("Title")).toHaveValue("New motion");
  expect(screen.queryByText("Motion created.")).not.toBeInTheDocument();
});

it("rejects duplicate options and backwards voting windows before submission", async () => {
  page();
  await screen.findByText("Approve budget");
  fillCreate();
  fireEvent.change(screen.getByLabelText("Option 2"), {
    target: { value: " approve " },
  });
  fireEvent.click(screen.getByRole("button", { name: "Create motion" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "distinct, non-empty options",
  );
  fireEvent.change(screen.getByLabelText("Option 2"), {
    target: { value: "Reject" },
  });
  fireEvent.change(screen.getByLabelText("Closes at"), {
    target: { value: "2020-01-01T10:00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Create motion" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "later closing time",
  );
  expect(post).not.toHaveBeenCalled();
});

it("shows loading, retryable list errors, and an empty state", async () => {
  get.mockRejectedValue(new Error("offline"));
  page();
  expect(screen.getByRole("status")).toHaveTextContent("Loading motions");
  expect(
    await screen.findByRole("button", { name: "Try again" }),
  ).toBeInTheDocument();
  get.mockResolvedValue({ data: [] });
  fireEvent.click(screen.getByRole("button", { name: "Try again" }));
  expect(await screen.findByText("No motions yet")).toBeInTheDocument();
});

it("reports member-loading errors when selecting voters", async () => {
  get.mockImplementation((path: string) =>
    path === "/motions"
      ? Promise.resolve({ data: motions })
      : Promise.reject(new Error("members offline")),
  );
  page();
  await screen.findByText("Approve budget");
  fireEvent.click(screen.getByLabelText("All active members may vote"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Motion request failed",
  );
  expect(screen.getByRole("button", { name: "Create motion" })).toBeDisabled();
});

it("refreshes server window state while the page stays open", async () => {
  vi.useFakeTimers();
  await act(async () => {
    page();
  });
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });
  motions = [{ ...draft(), state: "OPEN" }];
  await act(async () => {
    await vi.advanceTimersByTimeAsync(15_000);
  });
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });
  expect(screen.getByText("OPEN")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Edit motion" }),
  ).not.toBeInTheDocument();
});

it("uses a new query and clears the create form when switching clubs", async () => {
  page();
  await screen.findByText("Approve budget");
  fillCreate();
  motions = [];
  act(() =>
    useAuthStore.setState((state) => ({
      activeClub: { ...state.activeClub!, id: "club-2" },
    })),
  );
  expect(await screen.findByText("No motions yet")).toBeInTheDocument();
  expect(screen.queryByText("Approve budget")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Title")).toHaveValue("");
  expect(client.getQueryData(["motions", "club-2"])).toEqual([]);
});
