import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Category, CategoryRequest, Order, OrderStatus, Page, Product, ProductRequest, ProductVariant, ProductVariantRequest } from './models';

@Injectable({ providedIn: 'root' })
export class AdminCategoryService {
  private readonly http = inject(HttpClient);
  listVisible(): Observable<Category[]> { return this.http.get<Category[]>(`${environment.apiUrl}/categories`); }
  create(request: CategoryRequest): Observable<Category> { return this.http.post<Category>(`${environment.apiUrl}/admin/categories`, request); }
  update(id: number, request: CategoryRequest): Observable<Category> { return this.http.put<Category>(`${environment.apiUrl}/admin/categories/${id}`, request); }
  setActive(id: number, active: boolean): Observable<Category> { return this.http.patch<Category>(`${environment.apiUrl}/admin/categories/${id}/active`, null, { params: { active } }); }
}

@Injectable({ providedIn: 'root' })
export class AdminProductService {
  private readonly http = inject(HttpClient);
  listVisible(page = 0, size = 12, search = '', categoryId?: number): Observable<Page<Product>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'newest');
    if (search.trim()) params = params.set('search', search.trim());
    if (categoryId) params = params.set('categoryId', categoryId);
    return this.http.get<Page<Product>>(`${environment.apiUrl}/products`, { params });
  }
  getVisible(id: number): Observable<Product> { return this.http.get<Product>(`${environment.apiUrl}/products/${id}`); }
  create(request: ProductRequest): Observable<Product> { return this.http.post<Product>(`${environment.apiUrl}/admin/products`, request); }
  update(id: number, request: ProductRequest): Observable<Product> { return this.http.put<Product>(`${environment.apiUrl}/admin/products/${id}`, request); }
  setActive(id: number, active: boolean): Observable<Product> { return this.http.patch<Product>(`${environment.apiUrl}/admin/products/${id}/active`, null, { params: { active } }); }
  createVariant(productId: number, request: ProductVariantRequest): Observable<ProductVariant> { return this.http.post<ProductVariant>(`${environment.apiUrl}/admin/products/${productId}/variants`, request); }
  updateVariant(productId: number, variantId: number, request: ProductVariantRequest): Observable<ProductVariant> { return this.http.put<ProductVariant>(`${environment.apiUrl}/admin/products/${productId}/variants/${variantId}`, request); }
  setVariantActive(productId: number, variantId: number, active: boolean): Observable<ProductVariant> { return this.http.patch<ProductVariant>(`${environment.apiUrl}/admin/products/${productId}/variants/${variantId}/active`, null, { params: { active } }); }
}

@Injectable({ providedIn: 'root' })
export class AdminOrderService {
  private readonly http = inject(HttpClient);
  list(page = 0, size = 15): Observable<Page<Order>> { return this.http.get<Page<Order>>(`${environment.apiUrl}/admin/orders`, { params: { page, size, sort: 'createdAt,desc' } }); }
  get(id: number): Observable<Order> { return this.http.get<Order>(`${environment.apiUrl}/admin/orders/${id}`); }
  updateStatus(id: number, status: OrderStatus): Observable<Order> { return this.http.patch<Order>(`${environment.apiUrl}/admin/orders/${id}/status`, { status }); }
}
