import { fireEvent,render,screen,waitFor } from "@testing-library/react"
import { QueryClient,QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { beforeEach,expect,it,vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { NotificationsPage } from "./index"

const {get,put}=vi.hoisted(()=>({get:vi.fn(),put:vi.fn()}))
vi.mock("@/features/auth/auth-api",()=>({api:{get,put},errorMessage:()=>"Notification request failed"}))
function setup(){const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});render(<MemoryRouter><QueryClientProvider client={client}><NotificationsPage/></QueryClientProvider></MemoryRouter>)}
beforeEach(()=>{vi.resetAllMocks();useAuthStore.getState().setSession("token",{id:"member",email:"member@example.test"},{id:"club-1",name:"Club",clubType:"INVESTMENT_CLUB",administrator:false,permissions:[]});get.mockResolvedValue({data:[{id:"notice-1",type:"VOTE_OPEN",title:"Voting is open",message:"Budget vote is ready for your vote.",targetPath:"/voting",availableAt:"2026-09-13T08:00:00Z",readAt:null}]});put.mockResolvedValue({data:{}})})
it("renders unread notifications and marks one as read",async()=>{setup();expect(await screen.findByText("Voting is open")).toBeInTheDocument();expect(screen.getByRole("link",{name:"View details"})).toHaveAttribute("href","/voting");fireEvent.click(screen.getByRole("button",{name:"Mark read"}));await waitFor(()=>expect(put).toHaveBeenCalledWith("/notifications/notice-1/read"))})
it("marks the whole feed as read",async()=>{setup();await screen.findByText("Voting is open");fireEvent.click(screen.getByRole("button",{name:"Mark all as read"}));await waitFor(()=>expect(put).toHaveBeenCalledWith("/notifications/read-all"))})
