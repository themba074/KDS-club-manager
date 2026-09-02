import { useMutation,useQuery,useQueryClient } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"

export type ContributionExpectationStatus={scheduleVersionId:string;scheduleName:string;membershipId:string;memberName:string;dueDate:string;expected:number;paid:number;outstanding:number;currency:string}
export type LedgerLine={type:"EXPECTED"|"PAYMENT";activityDate:string;description:string;scheduleVersionId:string;paymentId:string|null;expected:number;paid:number;runningBalance:number;currency:string}
export type MemberLedger={membershipId:string;from:string;to:string;totalExpected:number;totalPaid:number;balance:number;currency:string;lines:LedgerLine[]}
export type PaymentInput={scheduleVersionId:string;membershipId:string;dueDate:string;amount:string;receivedOn:string;reference:string;note:string}

const club=()=>useAuthStore.getState().activeClub?.id
export function useRecordableExpectations(from:string,to:string,enabled:boolean){
  const id=useAuthStore(s=>s.activeClub?.id)
  return useQuery({queryKey:["payment-expectations",id,from,to],enabled:Boolean(id&&from&&to&&enabled),queryFn:({signal})=>api.get<ContributionExpectationStatus[]>("/contribution-payments/expectations",{signal,params:{from,to}}).then(r=>r.data)})
}
export function useMyLedger(from:string,to:string){
  const id=useAuthStore(s=>s.activeClub?.id)
  return useQuery({queryKey:["my-ledger",id,from,to],enabled:Boolean(id&&from&&to),queryFn:({signal})=>api.get<MemberLedger>("/contribution-payments/my-ledger",{signal,params:{from,to}}).then(r=>r.data)})
}
export function useRecordPayment(){
  const client=useQueryClient()
  return useMutation({mutationFn:({input,proof}:{input:PaymentInput;proof?:File})=>{
    const body=new FormData();body.append("payment",new Blob([JSON.stringify(input)],{type:"application/json"}));if(proof)body.append("proof",proof)
    return api.post("/contribution-payments",body)
  },onSuccess:()=>{void client.invalidateQueries({queryKey:["payment-expectations",club()]});void client.invalidateQueries({queryKey:["my-ledger",club()]})}})
}
