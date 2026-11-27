"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { callBackend } from "@/lib/backend-action";
import type { ActionResult } from "@/lib/types";

/** Changes the signed-in user's password. On success it goes to the dashboard. */
export async function changeMyPassword(input: {
  currentPassword: string;
  newPassword: string;
}): Promise<ActionResult> {
  const result = await callBackend<null>("/api/account/password", {
    method: "POST",
    body: JSON.stringify(input),
  });
  if (!result.ok) return result;

  revalidatePath("/", "layout"); // the "must change password" flag is gone now
  redirect("/dashboard");
}
