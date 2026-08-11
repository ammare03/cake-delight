"use client";

import { FormEvent, useState } from "react";
import { CakeFilters } from "@/lib/types";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";

const EMPTY_FILTERS: CakeFilters = { name: "", category: "", minPrice: "", maxPrice: "" };

export function CakeFiltersBar({ onApply }: { onApply: (filters: CakeFilters) => void }) {
  const [filters, setFilters] = useState<CakeFilters>(EMPTY_FILTERS);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onApply(filters);
  }

  function handleClear() {
    setFilters(EMPTY_FILTERS);
    onApply(EMPTY_FILTERS);
  }

  return (
    <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-4 sm:grid-cols-5 sm:items-end">
      <div className="col-span-2 space-y-1.5 sm:col-span-1">
        <Label htmlFor="filter-name">Name</Label>
        <Input
          id="filter-name"
          placeholder="e.g. Chocolate"
          value={filters.name}
          onChange={(e) => setFilters((f) => ({ ...f, name: e.target.value }))}
        />
      </div>
      <div className="col-span-2 space-y-1.5 sm:col-span-1">
        <Label htmlFor="filter-category">Category</Label>
        <Input
          id="filter-category"
          placeholder="e.g. fruit"
          value={filters.category}
          onChange={(e) => setFilters((f) => ({ ...f, category: e.target.value }))}
        />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="filter-min">Min price</Label>
        <Input
          id="filter-min"
          type="number"
          min={0}
          value={filters.minPrice}
          onChange={(e) => setFilters((f) => ({ ...f, minPrice: e.target.value }))}
        />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="filter-max">Max price</Label>
        <Input
          id="filter-max"
          type="number"
          min={0}
          value={filters.maxPrice}
          onChange={(e) => setFilters((f) => ({ ...f, maxPrice: e.target.value }))}
        />
      </div>
      <div className="flex gap-2">
        <Button type="submit" className="flex-1">
          Apply
        </Button>
        <Button type="button" variant="outline" onClick={handleClear}>
          Clear
        </Button>
      </div>
    </form>
  );
}
