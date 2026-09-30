export type Role = 'CUSTOMER' | 'ADMIN';
export type EggType = 'EGG' | 'EGGLESS';
export type OrderStatus = 'PLACED' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';

export interface User { id: number; name: string; email: string; role: Role; active: boolean; }
export interface AuthResponse { token: string; user: User; }
export interface Page<T> { content: T[]; number: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean; }
export interface Category { id: number; name: string; description: string | null; active: boolean; }
export interface CategoryRequest { name: string; description: string; }
export interface ProductVariant { id: number; productId: number; weightInGrams: number | null; flavour: string | null; eggType: EggType | null; price: number; active: boolean; }
export interface Product { id: number; name: string; description: string | null; imageUrl: string; categoryId: number; categoryName: string; active: boolean; variants: ProductVariant[]; }
export interface ProductRequest { name: string; description: string; imageUrl: string; categoryId: number; }
export interface ProductVariantRequest { weightInGrams: number | null; flavour: string; eggType: EggType | null; price: number; }
export interface OrderItem { id: number; productName: string; weightInGrams: number | null; flavour: string | null; eggType: EggType | null; quantity: number; unitPrice: number; }
export interface Order { id: number; orderNumber: string; status: OrderStatus; subtotal: number; deliveryFee: number; totalAmount: number; deliveryAddressLine1: string; deliveryCity: string; deliveryPhone: string; createdAt: string; items: OrderItem[]; }
export interface Payment { id: number; orderId: number; amount: number; status: string; paymentMethod: string; gateway: string; gatewayOrderId: string; gatewayPaymentId: string | null; }
