import { fireEvent,render,screen,waitFor } from "@testing-library/react"
import { QueryClient,QueryClientProvider } from "@tanstack/react-query"
import { beforeEach,expect,it,vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { ContributionsPage } from "./index"
const {get,post,put}=vi.hoisted(()=>({get:vi.fn(),post:vi.fn(),put:vi.fn()}))
vi.mock("@/features/auth/auth-api",()=>({api:{get,post,put},errorMessage:()=>"Schedule failed"}))
const member={membershipId:"member-1",email:"member@example.test",displayName:"Member One"}
const schedule={scheduleId:"schedule-1",versionId:"version-1",versionNumber:1,name:"Monthly savings",amount:100,currency:"ZAR",frequency:"MONTHLY",firstDueDate:"2026-09-01",endDate:null,effectiveFrom:"2026-08-29",effectiveTo:null,assignmentMode:"ALL_CURRENT",assignedMembers:[member]}
const ledger={membershipId:"member-1",from:"2026-01-01",to:"2026-12-31",totalExpected:100,totalPaid:40,balance:60,currency:"ZAR",lines:[]}
beforeEach(()=>{
  vi.clearAllMocks()
  useAuthStore.getState().setSession("token",{id:"owner",email:"owner@example.test"},{id:"club-1",name:"Club",clubType:"INVESTMENT_CLUB",administrator:true,permissions:["CONTRIBUTIONS_READ","CONTRIBUTIONS_WRITE"]})
  get.mockImplementation((path:string)=>Promise.resolve({data:path.endsWith("assignable-members")?[member]:path.endsWith("my-ledger")?ledger:path.endsWith("expectations")||path.endsWith("upcoming")?[]:[schedule]}))
  post.mockResolvedValue({data:schedule});put.mockResolvedValue({data:{...schedule,versionNumber:2}})
})
function page(){const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});render(<QueryClientProvider client={client}><ContributionsPage/></QueryClientProvider>);return client}
it("shows schedules and upcoming expectations to read-only members",async()=>{
  useAuthStore.setState(state=>({activeClub:{...state.activeClub!,administrator:false,permissions:["CONTRIBUTIONS_READ"]}}))
  page();expect(await screen.findByText("Monthly savings")).toBeInTheDocument();expect(screen.queryByRole("heading",{name:"Create schedule"})).not.toBeInTheDocument()
  expect(get.mock.calls.some(([path])=>path.endsWith("assignable-members"))).toBe(false)
})
it("creates an all-current-members schedule",async()=>{
  page();await screen.findByText("Monthly savings")
  fireEvent.change(screen.getByLabelText("Name"),{target:{value:"Annual joining fee"}})
  fireEvent.change(screen.getByLabelText("Amount (ZAR)"),{target:{value:"250.00"}})
  fireEvent.change(screen.getByLabelText("Frequency"),{target:{value:"ONCE_OFF"}})
  fireEvent.click(screen.getByRole("button",{name:"Create schedule"}))
  await waitFor(()=>expect(post).toHaveBeenCalledWith("/contribution-schedules",expect.objectContaining({name:"Annual joining fee",amount:"250.00",frequency:"ONCE_OFF",assignmentMode:"ALL_CURRENT",membershipIds:[],endDate:null})))
})
it("selects members and creates an immutable revision",async()=>{
  page();fireEvent.click(await screen.findByRole("button",{name:"Create revision"}))
  expect(screen.getByRole("heading",{name:"Create revision 2"})).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText("Assign to"),{target:{value:"SELECTED"}})
  const checkbox=await screen.findByRole("checkbox",{name:/Member One/});if(!(checkbox as HTMLInputElement).checked)fireEvent.click(checkbox)
  fireEvent.change(screen.getByLabelText("Amount (ZAR)"),{target:{value:"125.50"}})
  fireEvent.click(screen.getByRole("button",{name:"Save new revision"}))
  await waitFor(()=>expect(put).toHaveBeenCalledWith("/contribution-schedules/schedule-1",expect.objectContaining({amount:"125.50",assignmentMode:"SELECTED",membershipIds:["member-1"]})))
})
it("reports API failures and never reports a save",async()=>{
  post.mockRejectedValue(new Error("denied"));page();await screen.findByText("Monthly savings")
  fireEvent.change(screen.getByLabelText("Name"),{target:{value:"Failed schedule"}});fireEvent.change(screen.getByLabelText("Amount (ZAR)"),{target:{value:"10"}})
  fireEvent.click(screen.getByRole("button",{name:"Create schedule"}));expect(await screen.findByRole("alert")).toHaveTextContent("Schedule failed")
})
