import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth';
import { CartService } from '../core/api';

@Component({
  selector: 'app-customer-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="site-header">
      <a class="brand" routerLink="/home" aria-label="Miette home">MIETTE <span>patisserie</span></a>
      <button class="nav-toggle" type="button" (click)="menuOpen = !menuOpen" aria-label="Toggle navigation">☰</button>
      <nav [class.open]="menuOpen" aria-label="Main navigation">
        <a routerLink="/home" routerLinkActive="active" (click)="menuOpen = false">Home</a>
        <a routerLink="/products" routerLinkActive="active" (click)="menuOpen = false">Shop</a>
        <ng-container *ngIf="auth.isCustomer(); else guestLinks">
          <a routerLink="/orders" routerLinkActive="active" (click)="menuOpen = false">Orders</a>
          <a routerLink="/addresses" routerLinkActive="active" (click)="menuOpen = false">Addresses</a>
          <a routerLink="/cart" routerLinkActive="active" class="cart-link" (click)="menuOpen = false">Cart <span>{{ cart.count$ | async }}</span></a>
          <button class="link-button" type="button" (click)="logout()">Sign out</button>
        </ng-container>
        <ng-template #guestLinks><a routerLink="/login" routerLinkActive="active" (click)="menuOpen = false">Sign in</a></ng-template>
      </nav>
    </header>
    <main><router-outlet></router-outlet></main>
    <footer>
      <div><strong>MIETTE</strong><p>Small-batch pastries, made for unhurried moments.</p></div>
      <div><p>Freshly made · Thoughtfully packed · Locally delivered</p><small>© 2026 Miette Patisserie</small></div>
    </footer>
  `
})
export class CustomerLayout implements OnInit {
  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  private readonly router = inject(Router);
  menuOpen = false;
  ngOnInit(): void { if (this.auth.isCustomer()) this.cart.get().subscribe({ error: () => this.cart.clearCount() }); }
  logout(): void { this.auth.logout(); this.cart.clearCount(); void this.router.navigate(['/home']); }
}
