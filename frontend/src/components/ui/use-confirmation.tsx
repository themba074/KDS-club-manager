import { useRef, useState } from "react";
import { Button } from "./button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogTitle,
} from "./dialog";
import { useAuthStore } from "@/features/auth/auth-store";

type Review = {
  title: string;
  description: string;
  action: () => void;
  clubId?: string;
};

/** Capture the reviewed action; never execute a previous club's action after switching. */
export function useConfirmation() {
  const [review, setReview] = useState<Review | null>(null);
  const action = useRef<Review | null>(null);
  const clubId = useAuthStore((state) => state.activeClub?.id);
  const dismiss = () => {
    action.current = null;
    setReview(null);
  };
  const confirm = (title: string, description: string, execute: () => void) => {
    const next = { title, description, action: execute, clubId };
    action.current = next;
    setReview(next);
  };
  const confirmation = (
    <Dialog
      open={Boolean(review && review.clubId === clubId)}
      onOpenChange={(open) => {
        if (!open) dismiss();
      }}
    >
      <DialogContent>
        <DialogTitle>{review?.title}</DialogTitle>
        <DialogDescription>{review?.description}</DialogDescription>
        <DialogFooter>
          <Button type="button" variant="outline" autoFocus onClick={dismiss}>
            Go back
          </Button>
          <Button
            type="button"
            onClick={() => {
              const pending = action.current;
              dismiss();
              if (
                pending &&
                pending.clubId === useAuthStore.getState().activeClub?.id
              )
                pending.action();
            }}
          >
            Confirm
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
  return { confirm, confirmation };
}
