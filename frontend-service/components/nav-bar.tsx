"use client";

import Link from "next/link";
import { CakeSlice, ShoppingBag } from "lucide-react";
import { useAuth } from "@/context/auth-context";
import { useBasket } from "@/context/basket-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const publicLinks = [{ href: "/catalog", label: "Catalog" }];
const authedLinks = [
  { href: "/orders", label: "Orders" },
  { href: "/notifications", label: "Notifications" },
];

export function NavBar() {
  const { user, logout } = useAuth();
  const { itemCount } = useBasket();

  return (
    <header className="border-b border-border bg-card">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <Link href="/" className="flex items-center gap-2 font-heading text-xl font-semibold text-foreground">
          <CakeSlice className="size-5 text-primary" aria-hidden />
          Cake Delight
        </Link>

        <nav className="hidden items-center gap-6 text-sm font-medium text-muted-foreground sm:flex">
          {[...publicLinks, ...(user ? authedLinks : [])].map((link) => (
            <Link key={link.href} href={link.href} className="transition-colors hover:text-foreground">
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-3">
          {user ? (
            <>
              <Link href="/basket" className="relative">
                <Button variant="outline" size="icon" aria-label="Basket">
                  <ShoppingBag className="size-4" />
                </Button>
                {itemCount > 0 && (
                  <Badge className="absolute -right-2 -top-2 h-5 min-w-5 justify-center rounded-full px-1 text-xs">
                    {itemCount}
                  </Badge>
                )}
              </Link>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" className="text-sm">
                    {user.email}
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Signed in as {user.role.toLowerCase()}</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onSelect={logout}>Log out</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </>
          ) : (
            <>
              <Link href="/login">
                <Button variant="ghost">Log in</Button>
              </Link>
              <Link href="/register">
                <Button>Sign up</Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
