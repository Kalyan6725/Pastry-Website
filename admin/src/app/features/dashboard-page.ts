import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';
import { apiMessage } from '../core/auth';
import { AdminCategoryService, AdminOrderService, AdminProductService } from '../core/api';
import { Order } from '../core/models';

@Component({
  selector: 'app-dashboard-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <header class="admin-page-head"><div><span class="kicker">OVERVIEW</span><h1>Good day, bakery team.</h1><p>A concise view of the active catalog and current orders.</p></div><a class="admin-primary" routerLink="/products/new">Add product</a></header>
    <p class="alert" *ngIf="error">{{ error }} <button type="button" (click)="load()">Retry</button></p>
    <div class="metric-grid" [class.is-loading]="loading"><article><span>Visible products</span><strong>{{ productCount }}</strong><small>Active customer catalog</small></article><article><span>Visible categories</span><strong>{{ categoryCount }}</strong><small>Active customer categories</small></article><article><span>Total orders</span><strong>{{ orderCount }}</strong><small>All recorded orders</small></article><article><span>Needs attention</span><strong>{{ attentionCount }}</strong><small>Placed or confirmed on this page</small></article></div>
    <section class="admin-panel"><div class="panel-head"><div><span class="kicker">FULFILMENT</span><h2>Recent orders</h2></div><a routerLink="/orders">View all</a></div><p class="loading-state" *ngIf="loading">Loading workspace…</p><div class="empty-state" *ngIf="!loading && !recentOrders.length"><strong>No orders yet</strong><span>New orders will appear here.</span></div>
      <div class="table-wrap" *ngIf="recentOrders.length"><table><thead><tr><th>Order</th><th>Placed</th><th>Items</th><th>Total</th><th>Status</th><th></th></tr></thead><tbody><tr *ngFor="let order of recentOrders"><td><strong>{{ order.orderNumber }}</strong></td><td>{{ order.createdAt | date:'mediumDate' }}</td><td>{{ itemCount(order) }}</td><td>{{ order.totalAmount | currency:'INR' }}</td><td><span class="status-pill" [attr.data-status]="order.status">{{ label(order.status) }}</span></td><td><a class="row-action" [routerLink]="['/orders', order.id]">Open</a></td></tr></tbody></table></div>
    </section>
    <p class="contract-note">Dashboard catalog figures reflect active public records because the backend does not expose admin read endpoints for inactive products or categories.</p>`
})
export class DashboardPage implements OnInit {
  private readonly products = inject(AdminProductService); private readonly categories = inject(AdminCategoryService); private readonly orders = inject(AdminOrderService);
  productCount = 0; categoryCount = 0; orderCount = 0; attentionCount = 0; recentOrders: Order[] = []; loading = true; error = '';
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.error = ''; forkJoin({ products: this.products.listVisible(0, 1), categories: this.categories.listVisible(), orders: this.orders.list(0, 6) }).pipe(finalize(() => this.loading = false)).subscribe({ next: ({ products, categories, orders }) => { this.productCount = products.totalElements; this.categoryCount = categories.length; this.orderCount = orders.totalElements; this.recentOrders = orders.content; this.attentionCount = orders.content.filter((order) => order.status === 'PLACED' || order.status === 'CONFIRMED').length; }, error: (error: unknown) => this.error = apiMessage(error, 'Unable to load dashboard data.') }); }
  itemCount(order: Order): number { return order.items.reduce((sum, item) => sum + item.quantity, 0); }
  label(value: string): string { return value.replace(/_/g, ' '); }
}
