

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


export interface Cake {
  id: number;
  name: string;
  description: string;
  category: string;
  price: number;
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


export interface Notification {
  id: number;
  orderId: number;
  channel: string;
  status: string;
  payload: string;
  createdAt: string;
}


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
