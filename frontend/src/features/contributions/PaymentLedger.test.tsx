import { fireEvent,render,screen,waitFor } from "@testing-library/react"
import { QueryClient,QueryClientProvider } from "@tanstack/react-query"
import { beforeEach,expect,it,vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { MyLedger } from "./MyLedger"
import { RecordPayment } from "./RecordPayment"
const {get,post}=vi.hoisted(()=>({get:vi.fn(),post:vi.fn()}))
vi.mock("@/features/auth/auth-api",()=>({api:{get,post},errorMessage:()=>"Payment failed"}))
const expectation={scheduleVersionId:"version-1",scheduleName:"Monthly savings",membershipId:"member-1",memberName:"Member One",dueDate:"2026-09-01",expected:100,paid:40,outstanding:60,currency:"ZAR"}
const ledger={membershipId:"member-1",from:"2026-01-01",to:"2026-12-31",totalExpected:100,totalPaid:40,balance:60,currency:"ZAR",lines:[{type:"EXPECTED",activityDate:"2026-09-01",description:"Monthly savings",scheduleVersionId:"version-1",paymentId:null,expected:100,paid:0,runningBalance:100,currency:"ZAR"},{type:"PAYMENT",activityDate:"2026-09-02",description:"Payment received",scheduleVersionId:"version-1",paymentId:"payment-1",expected:0,paid:40,runningBalance:60,currency:"ZAR"}]}
beforeEach(()=>{vi.clearAllMocks();useAuthStore.getState().setSession("token",{id:"member-user",email:"member@example.test"},{id:"club-1",name:"Club",clubType:"INVESTMENT_CLUB",administrator:false,permissions:["CONTRIBUTIONS_READ","CONTRIBUTIONS_WRITE"]});get.mockImplementation((path:string)=>Promise.resolve({data:path.endsWith("my-ledger")?ledger:[expectation]}));post.mockResolvedValue({data:{}})})
function renderWithQuery(component:React.ReactNode){const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});render(<QueryClientProvider client={client}>{component}</QueryClientProvider>)}
it("renders the authenticated member ledger and running balance",async()=>{renderWithQuery(<MyLedger/>);expect((await screen.findAllByText("R 60.00")).length).toBeGreaterThan(0);expect(screen.getByText("Monthly savings")).toBeInTheDocument();expect(screen.getByText("Only your authenticated membership can be shown here.")).toBeInTheDocument()})
it("records the outstanding amount against the selected expectation",async()=>{renderWithQuery(<RecordPayment/>);expect(await screen.findByRole("option",{name:/Member One/})).toBeInTheDocument();fireEvent.click(screen.getByRole("button",{name:"Mark payment received"}));await waitFor(()=>expect(post).toHaveBeenCalledWith("/contribution-payments",expect.any(FormData)))})
