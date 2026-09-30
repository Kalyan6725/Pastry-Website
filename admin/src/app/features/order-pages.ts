import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiMessage } from '../core/auth';
import { AdminOrderService } from '../core/api';
import { Order, OrderStatus, Page } from '../core/models';

@Component({
  selector: 'app-admin-orders-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <header class="admin-page-head"><div><span class="kicker">FULFILMENT</span><h1>Orders</h1><p>Review incoming orders and move them through preparation and delivery.</p></div></header>
    <p class="contract-note">The current backend provides paginated orders but no status filter, customer-name search, or customer identity fields. This view intentionally shows only server-provided order and delivery data.</p><p class="alert" *ngIf="error">{{ error }} <button type="button" (click)="load()">Retry</button></p>
    <section class="admin-panel flush"><div class="panel-head table-panel-head"><div><h2>All orders</h2><span>{{ page?.totalElements || 0 }} total records</span></div><div class="legend"><span><i class="dot new"></i>New</span><span><i class="dot progress"></i>In progress</span><span><i class="dot complete"></i>Complete</span></div></div><p class="loading-state" *ngIf="loading">Loading orders…</p><div class="empty-state" *ngIf="!loading && !page?.content?.length"><strong>No orders recorded</strong><span>Customer orders will appear here as they are placed.</span></div>
      <div class="table-wrap" *ngIf="page?.content?.length"><table><thead><tr><th>Order</th><th>Placed</th><th>Delivery contact</th><th>Items</th><th>Amount</th><th>Status</th><th></th></tr></thead><tbody><tr *ngFor="let order of page?.content"><td><strong>{{ order.orderNumber }}</strong><small class="cell-sub">ID {{ order.id }}</small></td><td>{{ order.createdAt | date:'mediumDate' }}<small class="cell-sub">{{ order.createdAt | date:'shortTime' }}</small></td><td>{{ order.deliveryPhone }}<small class="cell-sub">{{ order.deliveryCity }}</small></td><td>{{ itemCount(order) }}</td><td><strong>{{ order.totalAmount | currency:'INR' }}</strong></td><td><span class="status-pill" [attr.data-status]="order.status">{{ label(order.status) }}</span></td><td><a class="row-action" [routerLink]="['/orders', order.id]">Review</a></td></tr></tbody></table></div>
      <div class="pager" *ngIf="page && page.totalPages > 1"><button type="button" [disabled]="page.first" (click)="load(page.number - 1)">Previous</button><span>Page {{ page.number + 1 }} of {{ page.totalPages }}</span><button type="button" [disabled]="page.last" (click)="load(page.number + 1)">Next</button></div>
    </section>`
})
export class AdminOrdersPage implements OnInit {
  private readonly api = inject(AdminOrderService); page: Page<Order> | null = null; loading = true; error = '';
  ngOnInit(): void { this.load(); }
  load(page = 0): void { this.loading = true; this.error = ''; this.api.list(page).pipe(finalize(() => this.loading = false)).subscribe({ next: (result) => this.page = result, error: (error: unknown) => this.error = apiMessage(error, 'Unable to load orders.') }); }
  itemCount(order: Order): number { return order.items.reduce((sum, item) => sum + item.quantity, 0); }
  label(value: string): string { return value.replace(/_/g, ' '); }
}

@Component({
  selector: 'app-admin-order-detail-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <header class="admin-page-head compact"><div><a class="back-link" routerLink="/orders">← Orders</a><span class="kicker">ORDER DETAIL</span><h1>{{ order?.orderNumber || 'Order' }}</h1><p *ngIf="order">Placed {{ order.createdAt | date:'fullDate' }} at {{ order.createdAt | date:'shortTime' }}</p></div><span class="status-pill large" *ngIf="order" [attr.data-status]="order.status">{{ label(order.status) }}</span></header>
    <p class="alert" role="alert" aria-live="assertive" *ngIf="error">{{ error }}</p><p class="loading-state" *ngIf="loading">Loading order details…</p>
    <ng-container *ngIf="order"><section class="order-command" *ngIf="allowedTransitions.length"><div><span class="kicker">NEXT ACTION</span><h2>Update fulfilment status</h2><p>Only transitions accepted by the backend are available.</p></div><div class="command-actions"><button class="admin-secondary danger" type="button" *ngIf="allowedTransitions.includes('CANCELLED')" [disabled]="updating" (click)="cancelConfirm = true">Cancel order</button><button class="admin-primary" type="button" *ngFor="let status of forwardTransitions" [disabled]="updating" (click)="updateStatus(status)">{{ updating ? 'Updating…' : actionLabel(status) }}</button></div></section>
      <div class="order-admin-grid"><div><section class="admin-panel"><div class="panel-head"><div><span class="kicker">CONTENTS</span><h2>Order items</h2></div><span>{{ itemCount }} items</span></div><div class="order-item" *ngFor="let item of order.items"><div class="item-monogram">{{ item.productName.charAt(0) }}</div><div><strong>{{ item.productName }}</strong><small>{{ variantLabel(item.weightInGrams, item.flavour, item.eggType) }}</small><span>{{ item.quantity }} × {{ item.unitPrice | currency:'INR' }}</span></div><strong>{{ item.quantity * item.unitPrice | currency:'INR' }}</strong></div></section>
        <section class="admin-panel"><div class="panel-head"><div><span class="kicker">DESTINATION</span><h2>Delivery</h2></div></div><div class="delivery-block"><span class="delivery-icon">⌖</span><div><strong>{{ order.deliveryAddressLine1 }}</strong><p>{{ order.deliveryCity }}</p></div></div><div class="delivery-block"><span class="delivery-icon">◉</span><div><small>Contact phone</small><strong>{{ order.deliveryPhone }}</strong></div></div><p class="contract-note inline">Customer name, email, full address snapshot fields, and external delivery tracking are not present in the current response DTO.</p></section></div>
        <aside><section class="admin-panel totals-panel"><span class="kicker">PAYMENT SUMMARY</span><h2>Order total</h2><div><span>Subtotal</span><strong>{{ order.subtotal | currency:'INR' }}</strong></div><div><span>Delivery fee</span><strong>{{ order.deliveryFee | currency:'INR' }}</strong></div><div class="total"><span>Total</span><strong>{{ order.totalAmount | currency:'INR' }}</strong></div><div class="payment-pending"><span>Payment integration pending</span><p>Payment records are customer-protected and no admin payment-read endpoint is currently available.</p></div></section>
          <section class="admin-panel timeline-panel"><span class="kicker">PROGRESS</span><h2>Fulfilment path</h2><ol><li *ngFor="let status of journey" [class.done]="isReached(status)" [class.current]="order.status === status"><span></span><div><strong>{{ label(status) }}</strong><small *ngIf="order.status === status">Current status</small></div></li></ol><div class="cancelled-note" *ngIf="order.status === 'CANCELLED'">Order cancelled</div></section></aside></div>
    </ng-container><div class="modal-backdrop" *ngIf="cancelConfirm" (click)="cancelConfirm = false"><section class="admin-modal confirm-modal" role="alertdialog" aria-modal="true" aria-labelledby="cancel-order-title" (click)="$event.stopPropagation()"><span class="kicker">TERMINAL ACTION</span><h2 id="cancel-order-title">Cancel {{ order?.orderNumber }}?</h2><p>The order status will become Cancelled and no further fulfilment transitions will be available.</p><div class="modal-actions"><button class="admin-secondary" type="button" (click)="cancelConfirm = false">Keep order</button><button class="admin-primary destructive" type="button" [disabled]="updating" (click)="updateStatus('CANCELLED')">{{ updating ? 'Cancelling…' : 'Cancel order' }}</button></div></section></div>`
})
export class AdminOrderDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute); private readonly api = inject(AdminOrderService);
  readonly journey: OrderStatus[] = ['PLACED', 'CONFIRMED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED'];
  readonly transitions: Record<OrderStatus, OrderStatus[]> = { PLACED: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['PREPARING', 'CANCELLED'], PREPARING: ['READY'], READY: ['OUT_FOR_DELIVERY'], OUT_FOR_DELIVERY: ['DELIVERED'], DELIVERED: [], CANCELLED: [] };
  order: Order | null = null; loading = true; updating = false; cancelConfirm = false; error = '';
  get allowedTransitions(): OrderStatus[] { return this.order ? this.transitions[this.order.status] : []; }
  get forwardTransitions(): OrderStatus[] { return this.allowedTransitions.filter((status) => status !== 'CANCELLED'); }
  get itemCount(): number { return this.order?.items.reduce((sum, item) => sum + item.quantity, 0) || 0; }
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; const id = Number(this.route.snapshot.paramMap.get('id')); this.api.get(id).pipe(finalize(() => this.loading = false)).subscribe({ next: (order) => this.order = order, error: (error: unknown) => this.error = apiMessage(error, 'Unable to load order details.') }); }
  updateStatus(status: OrderStatus): void { if (!this.order) return; this.updating = true; this.error = ''; this.api.updateStatus(this.order.id, status).pipe(finalize(() => this.updating = false)).subscribe({ next: (order) => { this.order = order; this.cancelConfirm = false; }, error: (error: unknown) => this.error = apiMessage(error, 'Unable to update order status.') }); }
  isReached(status: OrderStatus): boolean { if (!this.order || this.order.status === 'CANCELLED') return false; return this.journey.indexOf(status) <= this.journey.indexOf(this.order.status); }
  label(value: string): string { return value.split('_').map((word) => word.charAt(0) + word.slice(1).toLowerCase()).join(' '); }
  actionLabel(status: OrderStatus): string { const labels: Record<string, string> = { CONFIRMED: 'Confirm order', PREPARING: 'Start preparation', READY: 'Mark ready', OUT_FOR_DELIVERY: 'Send for delivery', DELIVERED: 'Mark delivered' }; return labels[status] || `Move to ${this.label(status)}`; }
  variantLabel(weight: number | null, flavour: string | null, eggType: string | null): string { return [weight ? `${weight}g` : '', flavour, eggType === 'EGGLESS' ? 'Eggless' : eggType === 'EGG' ? 'With egg' : ''].filter(Boolean).join(' · '); }
}
