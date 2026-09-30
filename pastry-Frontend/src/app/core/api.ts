import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Address, AddressRequest, Cart, CartItem, Category, Order, Page, Product, ProductQuery } from './models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  list(): Observable<Category[]> { return this.http.get<Category[]>(`${environment.apiUrl}/categories`); }
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  list(query: ProductQuery = {}): Observable<Page<Product>> {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') params = params.set(key, String(value));
    });
    return this.http.get<Page<Product>>(`${environment.apiUrl}/products`, { params });
  }
  get(id: number): Observable<Product> { return this.http.get<Product>(`${environment.apiUrl}/products/${id}`); }
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly countSubject = new BehaviorSubject(0);
  readonly count$ = this.countSubject.asObservable();
  get(): Observable<Cart> { return this.http.get<Cart>(`${environment.apiUrl}/cart`).pipe(tap((cart) => this.sync(cart))); }
  add(productVariantId: number, quantity: number): Observable<CartItem> {
    return this.http.post<CartItem>(`${environment.apiUrl}/cart/items`, { productVariantId, quantity });
  }
  update(id: number, quantity: number): Observable<CartItem> {
    return this.http.put<CartItem>(`${environment.apiUrl}/cart/items/${id}`, { quantity });
  }
  remove(id: number): Observable<void> { return this.http.delete<void>(`${environment.apiUrl}/cart/items/${id}`); }
  sync(cart: Cart): void { this.countSubject.next(cart.items.reduce((sum, item) => sum + item.quantity, 0)); }
  clearCount(): void { this.countSubject.next(0); }
}

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly http = inject(HttpClient);
  list(): Observable<Address[]> { return this.http.get<Address[]>(`${environment.apiUrl}/addresses`); }
  create(body: AddressRequest): Observable<Address> { return this.http.post<Address>(`${environment.apiUrl}/addresses`, body); }
  update(id: number, body: AddressRequest): Observable<Address> { return this.http.put<Address>(`${environment.apiUrl}/addresses/${id}`, body); }
  remove(id: number): Observable<void> { return this.http.delete<void>(`${environment.apiUrl}/addresses/${id}`); }
  setDefault(id: number): Observable<Address> { return this.http.patch<Address>(`${environment.apiUrl}/addresses/${id}/default`, {}); }
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  list(page = 0, size = 10): Observable<Page<Order>> {
    return this.http.get<Page<Order>>(`${environment.apiUrl}/orders`, { params: { page, size } });
  }
  get(id: number): Observable<Order> { return this.http.get<Order>(`${environment.apiUrl}/orders/${id}`); }
  create(addressId: number): Observable<Order> { return this.http.post<Order>(`${environment.apiUrl}/orders`, { addressId }); }
  cancel(id: number): Observable<Order> { return this.http.post<Order>(`${environment.apiUrl}/orders/${id}/cancel`, {}); }
}
