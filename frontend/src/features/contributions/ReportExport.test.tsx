import { fireEvent,render,screen,waitFor } from "@testing-library/react"
import { QueryClient,QueryClientProvider } from "@tanstack/react-query"
import { beforeEach,expect,it,vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { ReportExport } from "./ReportExport"
const {get}=vi.hoisted(()=>({get:vi.fn()}))
vi.mock("@/features/auth/auth-api",()=>({api:{get},errorMessage:()=>"Report failed"}))
const report={clubId:"club-1",clubName:"Ubuntu Club",from:"2026-01-01",to:"2026-12-31",generatedAt:"2026-09-03T08:00:00Z",totalExpected:100.1,totalCollected:40.05,totalOutstanding:60.05,currency:"ZAR",members:[{membershipId:"member-1",memberName:"Member One",memberEmail:"member@example.test",expected:100.1,collected:40.05,outstanding:60.05,currency:"ZAR"}]}
beforeEach(()=>{vi.clearAllMocks();useAuthStore.getState().setSession("token",{id:"owner",email:"owner@example.test"},{id:"club-1",name:"Club",clubType:"INVESTMENT_CLUB",administrator:true,permissions:["REPORTS_READ"]});Object.defineProperty(URL,"createObjectURL",{value:vi.fn(()=>"blob:report"),configurable:true});Object.defineProperty(URL,"revokeObjectURL",{value:vi.fn(),configurable:true});vi.spyOn(HTMLAnchorElement.prototype,"click").mockImplementation(()=>undefined);get.mockImplementation((path:string)=>Promise.resolve({data:path.endsWith("summary")?report:new Blob(["report"])}))})
function page(){const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});render(<QueryClientProvider client={client}><ReportExport/></QueryClientProvider>)}
it("shows the same cent-precise totals and member breakdown",async()=>{page();expect(await screen.findByText("Member One")).toBeInTheDocument();expect(screen.getAllByText("R 60.05").length).toBeGreaterThan(0);expect(screen.getByText("member@example.test")).toBeInTheDocument()})
it("downloads CSV through the report export endpoint",async()=>{page();await screen.findByText("Member One");fireEvent.click(screen.getByRole("button",{name:"Export CSV"}));await waitFor(()=>expect(get).toHaveBeenCalledWith("/contribution-reports/export",expect.objectContaining({params:expect.objectContaining({format:"CSV"}),responseType:"blob"})));expect(URL.createObjectURL).toHaveBeenCalled()})
