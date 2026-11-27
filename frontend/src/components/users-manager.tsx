"use client";

import { Check, Copy, KeyRound, Pencil, UserPlus, X } from "lucide-react";
import { type FormEvent, useState, useTransition } from "react";
import {
  activateUser,
  createUser,
  deactivateUser,
  resetUserPassword,
  updateUser,
} from "@/app/(app)/users/actions";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { brandButtonClass } from "@/lib/styles";
import type { ManagedUser } from "@/lib/types";

const ROLE_OPTIONS = [
  { value: "MANAGER", label: "Manager" },
  { value: "ENGINEER", label: "Engineer" },
  { value: "TECHNICIAN", label: "Technician" },
  { value: "CUSTOMER", label: "Customer" },
  { value: "ADMIN", label: "Administrator" },
];

const SELECT_CLASS = "h-9 w-full rounded-md border bg-transparent px-3 text-sm dark:bg-input/30";

type Issued = { name: string; email: string; password: string };

/** The role shown in the edit form (ADMIN wins when a user has several). */
function primaryRole(user: ManagedUser) {
  return user.roles.includes("ADMIN") ? "ADMIN" : (user.roles[0] ?? "CUSTOMER");
}

function text(data: FormData, name: string) {
  return String(data.get(name) ?? "");
}

/** Shows a one-time password once, with a copy button. */
function TemporaryPassword({ issued, onClose }: { issued: Issued; onClose: () => void }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(issued.password);
      setCopied(true);
    } catch {
      // clipboard blocked by the browser: the password is still visible to copy by hand
    }
  }

  return (
    <div
      role="status"
      className="rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm dark:border-amber-500/40 dark:bg-amber-500/10"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-medium">
            Temporary password for {issued.name} ({issued.email})
          </p>
          <p className="mt-1 text-muted-foreground">
            It is shown only once. Give it to the person privately. They must choose their own password
            when they first sign in.
          </p>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="rounded-md p-1 text-muted-foreground hover:bg-muted"
        >
          <X className="size-4" />
        </button>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <code className="select-all rounded-md border bg-card px-3 py-1.5 font-mono text-base tracking-wide">
          {issued.password}
        </code>
        <Button type="button" variant="outline" size="sm" onClick={copy}>
          {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
          {copied ? "Copied" : "Copy"}
        </Button>
      </div>
    </div>
  );
}

export function UsersManager({ users, currentUserId }: { users: ManagedUser[]; currentUserId: number }) {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<ManagedUser | null>(null);
  const [issued, setIssued] = useState<Issued | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [isPending, startTransition] = useTransition();

  function openCreate() {
    setEditing(null);
    setError(null);
    setShowForm((open) => !open);
  }

  function openEdit(user: ManagedUser) {
    setShowForm(false);
    setError(null);
    setEditing(user);
  }

  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setError(null);

    startTransition(async () => {
      const result = await createUser({
        firstName: text(data, "firstName"),
        lastName: text(data, "lastName"),
        email: text(data, "email"),
        phone: text(data, "phone"),
        role: text(data, "role"),
      });
      if (!result.ok) {
        setError(result.error);
        return;
      }
      const created = result.data.user;
      setIssued({
        name: `${created.firstName} ${created.lastName}`,
        email: created.email,
        password: result.data.temporaryPassword,
      });
      form.reset();
      setShowForm(false);
    });
  }

  function handleUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;
    const id = editing.id;
    const editingSelf = id === currentUserId;
    const data = new FormData(event.currentTarget);
    setError(null);

    startTransition(async () => {
      const result = await updateUser(id, {
        firstName: text(data, "firstName"),
        lastName: text(data, "lastName"),
        phone: text(data, "phone"),
        // admins cannot change their own role, so it is not sent for them
        role: editingSelf ? null : text(data, "role"),
      });
      if (!result.ok) {
        setError(result.error);
        return;
      }
      setEditing(null);
    });
  }

  function runRowAction(user: ManagedUser, action: "deactivate" | "activate" | "reset") {
    const name = `${user.firstName} ${user.lastName}`;
    const question =
      action === "deactivate"
        ? `Deactivate ${name}? They lose access at once and cannot sign in until reactivated.`
        : action === "reset"
          ? `Reset the password of ${name}? Their current password stops working at once.`
          : null;
    if (question && !window.confirm(question)) return;

    setError(null);
    setBusyId(user.id);
    startTransition(async () => {
      if (action === "reset") {
        const result = await resetUserPassword(user.id);
        if (result.ok) {
          setIssued({ name, email: user.email, password: result.data.temporaryPassword });
        } else {
          setError(result.error);
        }
      } else {
        const result = action === "deactivate" ? await deactivateUser(user.id) : await activateUser(user.id);
        if (!result.ok) setError(result.error);
      }
      setBusyId(null);
    });
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button type="button" onClick={openCreate} className={brandButtonClass} aria-expanded={showForm}>
          <UserPlus className="size-4" />
          Add user
        </Button>
      </div>

      {issued && <TemporaryPassword issued={issued} onClose={() => setIssued(null)} />}

      {error && (
        <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-400">
          {error}
        </p>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="grid gap-4 rounded-xl border bg-card p-5 sm:grid-cols-2">
          <h2 className="font-semibold sm:col-span-2">New user</h2>
          <div className="space-y-1.5">
            <Label htmlFor="firstName">First name</Label>
            <Input id="firstName" name="firstName" required maxLength={100} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="lastName">Last name</Label>
            <Input id="lastName" name="lastName" required maxLength={100} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input id="email" name="email" type="email" required maxLength={255} autoComplete="off" />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="phone">Phone (optional)</Label>
            <Input id="phone" name="phone" type="tel" maxLength={30} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="role">Role</Label>
            <select id="role" name="role" required defaultValue="ENGINEER" className={SELECT_CLASS}>
              {ROLE_OPTIONS.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-end justify-end gap-2 sm:col-span-2">
            <Button type="button" variant="outline" onClick={() => setShowForm(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isPending} className={brandButtonClass}>
              {isPending ? "Creating..." : "Create user"}
            </Button>
          </div>
        </form>
      )}

      {editing && (
        // key: a fresh form (with the right default values) for each user being edited
        <form
          key={editing.id}
          onSubmit={handleUpdate}
          className="grid gap-4 rounded-xl border bg-card p-5 sm:grid-cols-2"
        >
          <div className="sm:col-span-2">
            <h2 className="font-semibold">
              Edit {editing.firstName} {editing.lastName}
            </h2>
            <p className="text-xs text-muted-foreground">
              Email: {editing.email}. The email is the sign-in name and cannot be changed. If it is wrong,
              deactivate this account and create a new one.
            </p>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-firstName">First name</Label>
            <Input id="edit-firstName" name="firstName" required maxLength={100} defaultValue={editing.firstName} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-lastName">Last name</Label>
            <Input id="edit-lastName" name="lastName" required maxLength={100} defaultValue={editing.lastName} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-phone">Phone (optional)</Label>
            <Input id="edit-phone" name="phone" type="tel" maxLength={30} defaultValue={editing.phone ?? ""} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-role">Role</Label>
            {editing.id === currentUserId ? (
              <p className="pt-2 text-sm text-muted-foreground">
                {editing.roles.join(", ")}. You cannot change your own role.
              </p>
            ) : (
              <select id="edit-role" name="role" required defaultValue={primaryRole(editing)} className={SELECT_CLASS}>
                {ROLE_OPTIONS.map((role) => (
                  <option key={role.value} value={role.value}>
                    {role.label}
                  </option>
                ))}
              </select>
            )}
          </div>
          <div className="flex items-end justify-end gap-2 sm:col-span-2">
            <Button type="button" variant="outline" onClick={() => setEditing(null)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isPending} className={brandButtonClass}>
              {isPending ? "Saving..." : "Save changes"}
            </Button>
          </div>
        </form>
      )}

      <div className="rounded-xl border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((user) => {
              const isMe = user.id === currentUserId;
              const busy = busyId === user.id;
              return (
                <TableRow key={user.id}>
                  <TableCell>
                    <div className="font-medium">
                      {user.firstName} {user.lastName}
                      {isMe && <span className="ml-1.5 text-xs text-muted-foreground">(you)</span>}
                    </div>
                    {user.mustChangePassword && (
                      <div className="text-xs text-amber-700 dark:text-amber-400">Must change password</div>
                    )}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{user.email}</TableCell>
                  <TableCell>{user.roles.join(", ")}</TableCell>
                  <TableCell>
                    <StatusBadge value={user.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={isPending}
                        onClick={() => openEdit(user)}
                        aria-label={`Edit ${user.firstName} ${user.lastName}`}
                      >
                        <Pencil className="size-4" />
                        Edit
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={isPending}
                        onClick={() => runRowAction(user, "reset")}
                      >
                        <KeyRound className="size-4" />
                        {busy ? "Working..." : "Reset password"}
                      </Button>
                      {/* nobody can deactivate themselves (the backend refuses it too) */}
                      {!isMe &&
                        (user.status === "ACTIVE" ? (
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            disabled={isPending}
                            onClick={() => runRowAction(user, "deactivate")}
                            className="text-red-600 dark:text-red-400"
                          >
                            Deactivate
                          </Button>
                        ) : (
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            disabled={isPending}
                            onClick={() => runRowAction(user, "activate")}
                          >
                            Activate
                          </Button>
                        ))}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
