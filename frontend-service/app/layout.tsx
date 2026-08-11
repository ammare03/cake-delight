import type { Metadata } from "next";
import { Geist, Geist_Mono, Fraunces } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/auth-context";
import { BasketProvider } from "@/context/basket-context";
import { NavBar } from "@/components/nav-bar";
import { Toaster } from "@/components/ui/sonner";

const geistSans = Geist({
  variable: "--font-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// Serif display face for headings/logo only (see globals.css --font-heading)
// — everything else stays on the sans body font. One accent font, used
// sparingly, is the "classy" half of "minimal, classy" (CLAUDE.md's Phase 5
// brief); a whole second typeface family everywhere would be the opposite.
const fraunces = Fraunces({
  variable: "--font-serif",
  subsets: ["latin"],
  weight: ["500", "600"],
  style: ["normal", "italic"],
});

export const metadata: Metadata = {
  title: "Cake Delight",
  description: "Handmade cakes, ordered online.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${geistSans.variable} ${geistMono.variable} ${fraunces.variable} antialiased`}>
        <AuthProvider>
          <BasketProvider>
            <div className="flex min-h-screen flex-col">
              <NavBar />
              <main className="flex-1">{children}</main>
            </div>
            <Toaster position="bottom-right" />
          </BasketProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
