import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AdminAuthService } from '../core/auth';

@Component({
  selector: 'app-admin-layout', standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="admin-shell" [class.nav-open]="navOpen">
      <a class="skip-link" href="#admin-content">Skip to content</a>
      <aside class="sidebar">
        <a class="admin-brand" routerLink="/dashboard"><span class="brand-mark">M</span><span>MIETTE<small>ADMINISTRATION</small></span></a>
        <nav aria-label="Admin navigation">
          <span class="nav-label">Workspace</span>
          <a routerLink="/dashboard" routerLinkActive="active" (click)="navOpen = false"><span class="nav-icon">⌂</span>Dashboard</a>
          <a routerLink="/products" routerLinkActive="active" (click)="navOpen = false"><span class="nav-icon">□</span>Products</a>
          <a routerLink="/categories" routerLinkActive="active" (click)="navOpen = false"><span class="nav-icon">≡</span>Categories</a>
          <a routerLink="/orders" routerLinkActive="active" (click)="navOpen = false"><span class="nav-icon">≣</span>Orders</a>
        </nav>
        <div class="sidebar-user"><span class="avatar">{{ auth.user?.name?.charAt(0) || 'A' }}</span><div><strong>{{ auth.user?.name || 'Administrator' }}</strong><small>{{ auth.user?.email }}</small></div></div>
        <button class="sidebar-logout" type="button" (click)="logout()">Sign out</button>
      </aside>
      <button class="nav-scrim" type="button" aria-label="Close navigation" (click)="navOpen = false"></button>
      <div class="admin-workspace">
        <header class="topbar"><button class="menu-button" type="button" aria-label="Open navigation" (click)="navOpen = true">☰</button><div><span class="environment-dot"></span>Local workspace</div><span>{{ today | date:'EEEE, d MMMM' }}</span></header>
        <main class="admin-main" id="admin-content" tabindex="-1"><router-outlet></router-outlet></main>
      </div>
    </div>`
})
export class AdminLayout {
  readonly auth = inject(AdminAuthService);
  private readonly router = inject(Router);
  readonly today = new Date();
  navOpen = false;
  logout(): void { this.auth.logout(); void this.router.navigate(['/login']); }
}
