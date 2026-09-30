export type Role = 'CUSTOMER' | 'ADMIN';
export type EggType = 'EGG' | 'EGGLESS';
export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface ApiError { timestamp: string; status: number; message: string; path: string; }
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
export interface User { id: number; name: string; email: string; role: Role; active: boolean; }
export interface AuthResponse { token: string; user: User; }
export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { name: string; email: string; password: string; }
export interface Category { id: number; name: string; description: string | null; active: boolean; }
export interface ProductVariant {
  id: number;
  productId: number;
  weightInGrams: number | null;
  flavour: string | null;
  eggType: EggType | null;
  price: number;
  active: boolean;
}
export interface Product {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string;
  categoryId: number;
  categoryName: string;
  active: boolean;
  variants: ProductVariant[];
}
export interface ProductQuery {
  search?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  sort?: 'newest' | 'name_asc' | 'name_desc' | 'price_asc' | 'price_desc';
  page?: number;
  size?: number;
}
export interface CartItem {
  id: number;
  productVariantId: number;
  productName: string;
  variantLabel: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}
export interface Cart { id: number; items: CartItem[]; subtotal: number; }
export interface AddressRequest {
  label: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
}
export interface Address extends AddressRequest { id: number; }
export interface OrderItem {
  id: number;
  productName: string;
  weightInGrams: number | null;
  flavour: string | null;
  eggType: EggType | null;
  quantity: number;
  unitPrice: number;
}
export interface Order {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  subtotal: number;
  deliveryFee: number;
  totalAmount: number;
  deliveryAddressLine1: string;
  deliveryCity: string;
  deliveryPhone: string;
  createdAt: string;
  items: OrderItem[];
}
