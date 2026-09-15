import { fireEvent,render,screen,waitFor } from "@testing-library/react"
import { QueryClient,QueryClientProvider } from "@tanstack/react-query"
import { beforeEach,expect,it,vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { AuditLogViewer } from "./AuditLogViewer"

const {get}=vi.hoisted(()=>({get:vi.fn()}))
vi.mock("@/features/auth/auth-api",()=>({api:{get},errorMessage:()=>"Audit request failed"}))
function page(){const client=new QueryClient({defaultOptions:{queries:{retry:false}}});render(<QueryClientProvider client={client}><AuditLogViewer/></QueryClientProvider>)}
const entry={id:"entry-1",actorId:"11111111-1111-1111-1111-111111111111",action:"ROLE_ASSIGNED",entityType:"MEMBERSHIP",entityId:"22222222-2222-2222-2222-222222222222",previousValue:"MEMBER",newValue:"TREASURER",occurredAt:"2026-09-15T10:00:00Z"}
beforeEach(()=>{vi.resetAllMocks();useAuthStore.getState().setSession("token",{id:"owner",email:"owner@example.test"},{id:"club",name:"Club",clubType:"INVESTMENT_CLUB",administrator:true,permissions:["AUDIT_READ"]});get.mockResolvedValue({data:{content:[entry],totalElements:1,totalPages:1,page:0,size:25}})})
it("renders an immutable action with actor and before/after values",async()=>{page();expect(await screen.findByText(/Actor: 11111111/)).toBeInTheDocument();expect(screen.getByText("MEMBER → TREASURER")).toBeInTheDocument()})
it("filters by action and rejects invalid actor identifiers locally",async()=>{page();await screen.findByText("Role assigned");fireEvent.change(screen.getByRole("combobox",{name:"Action"}),{target:{value:"ROLE_ASSIGNED"}});await waitFor(()=>expect(get).toHaveBeenCalledWith("/audit-log",expect.objectContaining({params:expect.objectContaining({action:"ROLE_ASSIGNED"})})));const calls=get.mock.calls.length;fireEvent.change(screen.getByRole("textbox",{name:"Actor ID"}),{target:{value:"not-a-uuid"}});expect(screen.getByRole("alert")).toHaveTextContent("Enter a valid actor UUID.");await waitFor(()=>expect(get.mock.calls.length).toBe(calls))})
