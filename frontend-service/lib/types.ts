/**
 * TypeScript mirrors of every backend DTO this frontend talks to. Kept 1:1
 * with the Java records (field names, nullability) rather than reshaped —
 * same reasoning as CLAUDE.md's "no shared domain-model JAR" rule for the
 * Spring services: this is the frontend's own copy of the contract, read
 * directly from each service's dto/ package, not a generated client.
 */

// ---- auth-service ----

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  email: string;
  role: "CUSTOMER" | "ADMIN";
}

// ---- catalog-service ----

export interface Cake {
  id: number;
  name: string;
  description: string;
  category: string;
  price: number; // BigDecimal, no custom Jackson config anywhere -> serializes as a plain JSON number
  available: boolean;
  imageUrl: string | null;
  createdAt: string;
}

export interface CakeFilters {
  name?: string;
  category?: string;
  minPrice?: string;
  maxPrice?: string;
}

// ---- order-service ----

export interface AddBasketItemRequest {
  cakeId: number;
  quantity: number;
}

export interface UpdateBasketItemRequest {
  quantity: number;
}

export interface BasketItem {
  id: number;
  cakeId: number;
  cakeName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Basket {
  id: number;
  userId: number;
  items: BasketItem[];
  totalAmount: number;
}

export interface OrderItem {
  id: number;
  cakeId: number;
  cakeName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  userId: number;
  totalAmount: number;
  status: string;
  items: OrderItem[];
  createdAt: string;
}

// ---- rating-service ----

export interface CreateRatingRequest {
  cakeId: number;
  ratingValue: number;
  reviewText?: string;
}

export interface Rating {
  id: number;
  cakeId: number;
  userId: number;
  ratingValue: number;
  reviewText: string | null;
  createdAt: string;
}

export interface RatingSummary {
  averageRating: number;
  totalRatings: number;
}

// ---- notification-service ----

export interface Notification {
  id: number;
  orderId: number;
  channel: string;
  status: string;
  payload: string;
  createdAt: string;
}

// ---- shared error shape (api-conventions skill) ----

export interface ValidationFieldError {
  field: string;
  message: string;
}

export interface ErrorResponseBody {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors: ValidationFieldError[] | null;
}
