import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiMessage } from '../core/auth';
import { AddressService, CartService, OrderService } from '../core/api';
import { Address, AddressRequest, Cart, Order, OrderStatus, Page } from '../core/models';

@Component({
  selector: 'app-cart-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <section class="page-head"><span class="eyebrow">YOUR SELECTION</span><h1>Shopping cart</h1></section>
    <section class="section narrow"><p class="loading" *ngIf="loading">Preparing your cart…</p><p class="error" *ngIf="error">{{ error }} <button type="button" (click)="load()">Retry</button></p>
      <div class="empty" *ngIf="!loading && cart && !cart.items.length"><h2>Your cart is empty</h2><p>There are plenty of fresh pastries at the counter.</p><a class="primary" routerLink="/products">Browse pastries</a></div>
      <ng-container *ngIf="cart && cart.items.length"><div class="cart-list"><article class="cart-row" *ngFor="let item of cart.items"><div><h3>{{ item.productName }}</h3><p>{{ item.variantLabel }}</p><strong>{{ item.unitPrice | currency:'INR' }} each</strong></div><div class="stepper"><button type="button" [attr.aria-label]="'Decrease quantity of ' + item.productName" [disabled]="busy" (click)="change(item.id, item.quantity - 1)">−</button><span aria-live="polite">{{ item.quantity }}</span><button type="button" [attr.aria-label]="'Increase quantity of ' + item.productName" [disabled]="busy" (click)="change(item.id, item.quantity + 1)">+</button></div><strong>{{ item.lineTotal | currency:'INR' }}</strong><button class="danger-link" type="button" [disabled]="busy" (click)="remove(item.id)">Remove</button></article></div>
        <div class="cart-summary"><span>Subtotal</span><strong>{{ cart.subtotal | currency:'INR' }}</strong><small>Delivery is calculated by the bakery when your order is created.</small><a class="primary full" routerLink="/checkout">Continue to checkout</a></div></ng-container>
    </section>`
})
export class CartPage implements OnInit {
  private readonly api = inject(CartService); cart: Cart | null = null; loading = true; busy = false; error = '';
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.error = ''; this.api.get().pipe(finalize(() => this.loading = false)).subscribe({ next: (cart) => this.cart = cart, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load your cart.') }); }
  change(id: number, quantity: number): void { if (quantity < 1) { this.remove(id); return; } this.busy = true; this.api.update(id, quantity).pipe(finalize(() => this.busy = false)).subscribe({ next: () => this.load(), error: (e: unknown) => this.error = apiMessage(e, 'Unable to update quantity.') }); }
  remove(id: number): void { this.busy = true; this.api.remove(id).pipe(finalize(() => this.busy = false)).subscribe({ next: () => this.load(), error: (e: unknown) => this.error = apiMessage(e, 'Unable to remove this item.') }); }
}

@Component({
  selector: 'app-addresses-page', standalone: true, imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="page-head"><span class="eyebrow">YOUR ACCOUNT</span><h1>Delivery addresses</h1></section>
    <section class="section narrow"><div class="section-heading"><div><h2>Saved addresses</h2><p>Choose one default destination for faster checkout.</p></div><button class="primary" type="button" (click)="beginCreate()">Add address</button></div>
      <p class="loading" *ngIf="loading">Loading addresses…</p><p class="error" *ngIf="error">{{ error }}</p><div class="empty" *ngIf="!loading && !addresses.length && !editing"><h2>No saved addresses</h2><p>Add your first delivery address.</p></div>
      <div class="address-grid"><article class="address-card" *ngFor="let item of addresses"><span class="badge" *ngIf="item.isDefault">Default</span><h3>{{ item.label || 'Address' }}</h3><p>{{ item.addressLine1 }}<br><span *ngIf="item.addressLine2">{{ item.addressLine2 }}<br></span>{{ item.city }}, {{ item.state }} {{ item.postalCode }}<br>{{ item.country }}</p><p>Phone: {{ item.phone }}</p><div class="actions"><button type="button" (click)="beginEdit(item)">Edit</button><button type="button" *ngIf="!item.isDefault" (click)="setDefault(item.id)">Make default</button><button class="danger-link" type="button" (click)="pendingDelete = item">Delete</button></div></article></div>
      <section class="address-confirm" role="alertdialog" aria-labelledby="delete-address-title" *ngIf="pendingDelete"><div><h2 id="delete-address-title">Delete {{ pendingDelete.label || 'this address' }}?</h2><p>{{ pendingDelete.addressLine1 }}, {{ pendingDelete.city }} will be removed from your account.</p></div><div><button class="secondary" type="button" (click)="pendingDelete = null">Keep address</button><button class="danger-action" type="button" (click)="confirmRemove()">Delete address</button></div></section>
      <form class="address-form" *ngIf="editing" [formGroup]="form" (ngSubmit)="save()" role="region" aria-labelledby="address-form-title"><div class="section-heading"><h2 id="address-form-title">{{ editId ? 'Edit address' : 'New address' }}</h2><button type="button" (click)="cancelEdit()">Close</button></div><div class="form-grid">
        <label>Label (optional)<input formControlName="label" maxlength="50" placeholder="Home or work"></label><label>Phone *<input formControlName="phone" maxlength="20" autocomplete="tel"></label><label class="wide">Address line 1 *<input formControlName="addressLine1" maxlength="255" autocomplete="address-line1"></label><label class="wide">Address line 2<input formControlName="addressLine2" maxlength="255" autocomplete="address-line2"></label><label>City *<input formControlName="city" maxlength="100" autocomplete="address-level2"></label><label>State *<input formControlName="state" maxlength="100" autocomplete="address-level1"></label><label>Postal code *<input formControlName="postalCode" maxlength="20" autocomplete="postal-code"></label><label>Country *<input formControlName="country" maxlength="100" autocomplete="country-name"></label><label class="check"><input type="checkbox" formControlName="isDefault"> Set as default</label></div><button class="primary" type="submit" [disabled]="form.invalid || saving">{{ saving ? 'Saving…' : 'Save address' }}</button></form>
    </section>`
})
export class AddressesPage implements OnInit {
  private readonly api = inject(AddressService); private readonly fb = inject(FormBuilder);
  addresses: Address[] = []; loading = true; saving = false; editing = false; editId: number | null = null; pendingDelete: Address | null = null; error = '';
  readonly form = this.fb.nonNullable.group({ label: ['', Validators.maxLength(50)], phone: ['', [Validators.required, Validators.maxLength(20)]], addressLine1: ['', [Validators.required, Validators.maxLength(255)]], addressLine2: ['', Validators.maxLength(255)], city: ['', [Validators.required, Validators.maxLength(100)]], state: ['', [Validators.required, Validators.maxLength(100)]], postalCode: ['', [Validators.required, Validators.maxLength(20)]], country: ['India', [Validators.required, Validators.maxLength(100)]], isDefault: [false] });
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.api.list().pipe(finalize(() => this.loading = false)).subscribe({ next: (items) => this.addresses = items, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load addresses.') }); }
  beginCreate(): void { this.editId = null; this.editing = true; this.form.reset({ label:'', phone:'', addressLine1:'', addressLine2:'', city:'', state:'', postalCode:'', country:'India', isDefault:!this.addresses.length }); }
  beginEdit(item: Address): void { this.editId = item.id; this.editing = true; this.form.reset(item); }
  cancelEdit(): void { this.editing = false; this.editId = null; }
  save(): void { if (this.form.invalid) return; this.saving = true; this.error = ''; const body: AddressRequest = this.form.getRawValue(); const request = this.editId ? this.api.update(this.editId, body) : this.api.create(body); request.pipe(finalize(() => this.saving = false)).subscribe({ next: () => { this.cancelEdit(); this.load(); }, error: (e: unknown) => this.error = apiMessage(e, 'Unable to save address.') }); }
  confirmRemove(): void { if (!this.pendingDelete) return; this.api.remove(this.pendingDelete.id).subscribe({ next: () => { this.pendingDelete = null; this.load(); }, error: (e: unknown) => this.error = apiMessage(e, 'Unable to delete address.') }); }
  setDefault(id: number): void { this.api.setDefault(id).subscribe({ next: () => this.load(), error: (e: unknown) => this.error = apiMessage(e, 'Unable to set the default address.') }); }
}

@Component({
  selector: 'app-payment-placeholder', standalone: true,
  template: `<section class="payment-placeholder"><span class="step-number">3</span><div><h2>Payment</h2><p>Third-party payment integration will be available soon.</p><button type="button" disabled>Payment integration pending</button></div></section>`
})
export class PaymentPlaceholder {}

@Component({
  selector: 'app-checkout-page', standalone: true, imports: [CommonModule, RouterLink, PaymentPlaceholder],
  template: `
    <section class="page-head"><span class="eyebrow">FINAL DETAILS</span><h1>Checkout</h1></section><section class="section checkout"><div>
      <section class="checkout-step"><span class="step-number">1</span><div><h2>Delivery address</h2><p class="loading" *ngIf="loading">Loading checkout…</p><p class="error" role="alert" *ngIf="error">{{ error }}</p><div class="checkout-guidance" *ngIf="!loading && !addresses.length"><strong>Add a delivery address to continue</strong><p>Phone and full delivery details are required before placing an order.</p><a class="secondary" routerLink="/addresses">Add an address</a></div><div class="address-options"><label *ngFor="let item of addresses" [class.selected]="addressId === item.id"><input type="radio" name="address" [value]="item.id" [checked]="addressId === item.id" (change)="addressId = item.id"><strong>{{ item.label || 'Address' }} <span *ngIf="item.isDefault">· Default</span></strong><span>{{ item.addressLine1 }}, {{ item.city }} · {{ item.phone }}</span></label></div><a class="text-link" *ngIf="addresses.length" routerLink="/addresses">Manage addresses →</a></div></section>
      <section class="checkout-step"><span class="step-number">2</span><div><h2>Order summary</h2><div class="summary-item" *ngFor="let item of cart?.items"><span>{{ item.productName }} × {{ item.quantity }}<small>{{ item.variantLabel }}</small></span><strong>{{ item.lineTotal | currency:'INR' }}</strong></div></div></section><app-payment-placeholder></app-payment-placeholder>
    </div><aside class="order-total"><h2>Your order</h2><div><span>Subtotal</span><strong>{{ cart?.subtotal | currency:'INR' }}</strong></div><p>The backend will calculate the authoritative delivery fee and final total when the order is created.</p><div class="checkout-guidance compact" *ngIf="!loading && !cart?.items?.length"><strong>Your cart is empty</strong><a routerLink="/products">Return to the collection</a></div><button class="primary full" type="button" [disabled]="!addressId || !cart?.items?.length || creating" (click)="placeOrder()">{{ creating ? 'Creating order…' : 'Place order' }}</button><small>No payment will be attempted.</small></aside></section>`
})
export class CheckoutPage implements OnInit {
  private readonly addressesApi = inject(AddressService); private readonly cartApi = inject(CartService); private readonly orders = inject(OrderService); private readonly router = inject(Router);
  addresses: Address[] = []; cart: Cart | null = null; addressId: number | null = null; loading = true; creating = false; error = '';
  ngOnInit(): void { let pending = 2; const done = () => { pending -= 1; if (!pending) this.loading = false; }; this.addressesApi.list().pipe(finalize(done)).subscribe({ next: (items) => { this.addresses = items; this.addressId = items.find((item) => item.isDefault)?.id || items[0]?.id || null; }, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load addresses.') }); this.cartApi.get().pipe(finalize(done)).subscribe({ next: (cart) => this.cart = cart, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load cart.') }); }
  placeOrder(): void { if (!this.addressId) return; this.creating = true; this.error = ''; this.orders.create(this.addressId).pipe(finalize(() => this.creating = false)).subscribe({ next: (order) => { this.cartApi.clearCount(); void this.router.navigate(['/orders', order.id], { queryParams: { created: true } }); }, error: (e: unknown) => this.error = apiMessage(e, 'Unable to create your order.') }); }
}

@Component({
  selector: 'app-orders-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `<section class="page-head"><span class="eyebrow">YOUR ACCOUNT</span><h1>My orders</h1></section><section class="section narrow"><p class="loading" *ngIf="loading">Loading orders…</p><p class="error" role="alert" *ngIf="error">{{ error }}</p><div class="empty" *ngIf="!loading && !page?.content?.length"><h2>No orders yet</h2><p>Your first pastry order is only a few clicks away.</p><a class="primary" routerLink="/products">Start shopping</a></div><div class="order-list"><article *ngFor="let order of page?.content"><div><span class="eyebrow">{{ order.createdAt | date:'mediumDate' }}</span><h2>{{ order.orderNumber }}</h2><p>{{ itemSummary(order) }}</p></div><div><span class="status" [attr.data-status]="order.status">{{ statusLabel(order.status) }}</span><strong>{{ order.totalAmount | currency:'INR' }}</strong><a [routerLink]="['/orders', order.id]">View order →</a></div></article></div><div class="pagination" *ngIf="page && page.totalPages > 1"><button aria-label="Previous orders page" [disabled]="page.first" (click)="load(page.number - 1)">Previous</button><span>Page {{ page.number + 1 }} of {{ page.totalPages }}</span><button aria-label="Next orders page" [disabled]="page.last" (click)="load(page.number + 1)">Next</button></div></section>`
})
export class OrdersPage implements OnInit {
  private readonly api = inject(OrderService); page: Page<Order> | null = null; loading = true; error = '';
  ngOnInit(): void { this.load(); }
  load(page = 0): void { this.loading = true; this.api.list(page).pipe(finalize(() => this.loading = false)).subscribe({ next: (result) => this.page = result, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load orders.') }); }
  itemSummary(order: Order): string { return order.items.map((item) => `${item.productName} × ${item.quantity}`).join(', '); }
  statusLabel(status: OrderStatus): string { return status.split('_').map((word) => word.charAt(0) + word.slice(1).toLowerCase()).join(' '); }
}

@Component({
  selector: 'app-order-detail-page', standalone: true, imports: [CommonModule, RouterLink],
  template: `<section class="page-head"><span class="eyebrow">ORDER TRACKING</span><h1>{{ order?.orderNumber || 'Order' }}</h1><p *ngIf="order">Placed {{ order.createdAt | date:'medium' }}</p></section><section class="section narrow"><p class="success" role="status" *ngIf="route.snapshot.queryParamMap.has('created')">Your order was created. Payment integration is pending; no payment was attempted.</p><p class="loading" *ngIf="loading">Loading order…</p><p class="error" role="alert" *ngIf="error">{{ error }}</p><ng-container *ngIf="order"><div class="tracker" *ngIf="order.status !== 'CANCELLED'"><div *ngFor="let status of journey; let i = index" [class.complete]="isComplete(status)" [class.current]="order.status === status"><span>{{ i + 1 }}</span><small>{{ statusLabel(status) }}</small></div></div><div class="cancelled" *ngIf="order.status === 'CANCELLED'">This order was cancelled.</div><div class="order-detail-grid"><div><h2>Items</h2><article class="summary-item" *ngFor="let item of order.items"><span><strong>{{ item.productName }} × {{ item.quantity }}</strong><small>{{ variant(item.weightInGrams, item.flavour, item.eggType) }}</small></span><strong>{{ item.unitPrice * item.quantity | currency:'INR' }}</strong></article><h2>Delivery</h2><p>{{ order.deliveryAddressLine1 }}<br>{{ order.deliveryCity }}<br>{{ order.deliveryPhone }}</p></div><aside class="order-total"><h2>Total</h2><div><span>Subtotal</span><strong>{{ order.subtotal | currency:'INR' }}</strong></div><div><span>Delivery</span><strong>{{ order.deliveryFee | currency:'INR' }}</strong></div><div class="grand"><span>Total</span><strong>{{ order.totalAmount | currency:'INR' }}</strong></div><div class="cancel-confirm" *ngIf="cancelConfirm"><strong>Cancel this order?</strong><p>This action cannot be undone.</p><div><button class="secondary" type="button" (click)="cancelConfirm = false">Keep order</button><button class="danger-action" type="button" [disabled]="cancelling" (click)="confirmCancel()">{{ cancelling ? 'Cancelling…' : 'Yes, cancel' }}</button></div></div><button class="secondary full" type="button" *ngIf="canCancel && !cancelConfirm" (click)="cancelConfirm = true">Cancel order</button></aside></div></ng-container></section>`
})
export class OrderDetailPage implements OnInit {
  readonly route = inject(ActivatedRoute); private readonly api = inject(OrderService);
  readonly journey: OrderStatus[] = ['PLACED','CONFIRMED','PREPARING','READY','OUT_FOR_DELIVERY','DELIVERED']; order: Order | null = null; loading = true; cancelling = false; cancelConfirm = false; error = '';
  get canCancel(): boolean { return this.order?.status === 'PLACED' || this.order?.status === 'CONFIRMED'; }
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; const id = Number(this.route.snapshot.paramMap.get('id')); this.api.get(id).pipe(finalize(() => this.loading = false)).subscribe({ next: (order) => this.order = order, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load this order.') }); }
  isComplete(status: OrderStatus): boolean { if (!this.order) return false; return this.journey.indexOf(status) <= this.journey.indexOf(this.order.status); }
  variant(weight: number | null, flavour: string | null, egg: string | null): string { return [weight ? `${weight}g` : '', flavour, egg === 'EGGLESS' ? 'Eggless' : egg === 'EGG' ? 'With egg' : ''].filter(Boolean).join(' · '); }
  statusLabel(status: OrderStatus): string { return status.split('_').map((word) => word.charAt(0) + word.slice(1).toLowerCase()).join(' '); }
  confirmCancel(): void { if (!this.order) return; this.cancelling = true; this.api.cancel(this.order.id).pipe(finalize(() => this.cancelling = false)).subscribe({ next: (order) => { this.order = order; this.cancelConfirm = false; }, error: (e: unknown) => this.error = apiMessage(e, 'Unable to cancel this order.') }); }
}
