import { CakeDetailClient } from "./cake-detail-client";

export default async function CakeDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CakeDetailClient cakeId={Number(id)} />;
}
