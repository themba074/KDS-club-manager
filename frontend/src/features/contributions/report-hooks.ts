import { useMutation,useQuery } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export type MemberContributionSummary={membershipId:string;memberName:string;memberEmail:string;expected:number;collected:number;outstanding:number;currency:string}
export type ContributionReport={clubId:string;clubName:string;from:string;to:string;generatedAt:string;totalExpected:number;totalCollected:number;totalOutstanding:number;currency:string;members:MemberContributionSummary[]}
export type ReportFormat="CSV"|"PDF"

export function useContributionReport(from:string,to:string){
  const clubId=useAuthStore(state=>state.activeClub?.id)
  return useQuery({queryKey:["contribution-report",clubId,from,to],enabled:Boolean(clubId&&from&&to),queryFn:({signal})=>api.get<ContributionReport>("/contribution-reports/summary",{signal,params:{from,to}}).then(response=>response.data)})
}
export function useContributionReportExport(){
  return useMutation({mutationFn:async({from,to,format}:{from:string;to:string;format:ReportFormat})=>{
    const response=await api.get<Blob>("/contribution-reports/export",{params:{from,to,format},responseType:"blob"})
    const url=URL.createObjectURL(response.data);const link=document.createElement("a");link.href=url;link.download=`contributions-${from}-to-${to}.${format.toLowerCase()}`;link.click();URL.revokeObjectURL(url)
  }})
}
