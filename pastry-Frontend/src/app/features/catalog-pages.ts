import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, Subject, takeUntil } from 'rxjs';
import { apiMessage, AuthService } from '../core/auth';
import { CartService, CategoryService, ProductService } from '../core/api';
import { Category, Page, Product, ProductQuery, ProductVariant } from '../core/models';
import { ProductCard } from '../shared/product-card';

@Component({
  selector: 'app-home-page', standalone: true, imports: [CommonModule, RouterLink, ProductCard],
  template: `
    <section class="hero">
      <div class="hero-copy"><span class="eyebrow">BAKED FRESH, EVERY MORNING</span><h1>Pastries worth<br>slowing down for.</h1><p>Small-batch cakes and confections made with honest ingredients and a light hand.</p><a class="primary" routerLink="/products">Explore the collection</a></div>
    </section>
    <section class="section"><div class="section-heading"><div><span class="eyebrow">FIND YOUR FAVOURITE</span><h2>Made for every craving</h2></div><a routerLink="/products">Shop all →</a></div>
      <p class="loading" *ngIf="loading">Loading today’s selection…</p><p class="error" *ngIf="error">{{ error }}</p>
      <div class="category-grid"><a class="category-tile" *ngFor="let category of categories; let index = index" routerLink="/products" [queryParams]="{categoryId: category.id}" [class.alt]="index % 2"><span>0{{ index + 1 }}</span><h3>{{ category.name }}</h3><p>{{ category.description }}</p></a></div>
    </section>
    <section class="section muted"><div class="section-heading"><div><span class="eyebrow">FROM THE COUNTER</span><h2>Today’s favourites</h2></div></div><div class="product-grid"><app-product-card *ngFor="let product of products" [product]="product"></app-product-card></div></section>
    <section class="promise"><div><strong>Made each morning</strong><span>Never mass-produced</span></div><div><strong>Real ingredients</strong><span>No unnecessary shortcuts</span></div><div><strong>Delivered with care</strong><span>Fresh to your door</span></div></section>`
})
export class HomePage implements OnInit {
  private readonly categoriesApi = inject(CategoryService); private readonly productsApi = inject(ProductService);
  categories: Category[] = []; products: Product[] = []; loading = true; error = '';
  ngOnInit(): void {
    this.categoriesApi.list().subscribe({ next: (items) => this.categories = items.filter((item) => item.active), error: (e: unknown) => this.error = apiMessage(e, 'Unable to load categories.') });
    this.productsApi.list({ size: 4, sort: 'newest' }).pipe(finalize(() => this.loading = false)).subscribe({ next: (page) => this.products = page.content, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load products.') });
  }
}

@Component({
  selector: 'app-products-page', standalone: true, imports: [CommonModule, FormsModule, ProductCard],
  template: `
    <section class="page-head"><span class="eyebrow">THE COLLECTION</span><h1>Shop all pastries</h1><p>Made in small batches and available while fresh.</p></section>
    <section class="catalog section">
      <button class="filter-toggle" type="button" [attr.aria-expanded]="filtersOpen" aria-controls="catalog-filters" (click)="filtersOpen = !filtersOpen">{{ filtersOpen ? 'Close filters' : 'Filters' }}</button>
      <aside id="catalog-filters" [class.open]="filtersOpen"><div class="aside-head"><h2>Filters</h2><button type="button" (click)="clear()">Clear</button></div>
        <label>Search<input [(ngModel)]="query.search" (ngModelChange)="search$.next($event)" placeholder="Cake, cookie, flavour…"></label>
        <label>Category<select [(ngModel)]="query.categoryId" (change)="apply()"><option [ngValue]="undefined">All categories</option><option *ngFor="let item of categories" [ngValue]="item.id">{{ item.name }}</option></select></label>
        <div class="price-row"><label>Minimum ₹<input type="number" min="0" [(ngModel)]="query.minPrice"></label><label>Maximum ₹<input type="number" min="0" [(ngModel)]="query.maxPrice"></label></div>
        <button class="secondary full" type="button" (click)="apply()">Apply filters</button>
      </aside>
      <div class="results"><div class="result-bar"><span>{{ page?.totalElements || 0 }} pastries</span><label>Sort <select [(ngModel)]="query.sort" (change)="apply()"><option value="newest">Newest</option><option value="name_asc">Name: A–Z</option><option value="name_desc">Name: Z–A</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option></select></label></div>
        <p class="loading" *ngIf="loading">Loading pastries…</p><div class="empty" *ngIf="!loading && !error && !page?.content?.length"><h2>No pastries found</h2><p>Try changing your search or filters.</p></div><p class="error" *ngIf="error">{{ error }} <button type="button" (click)="load()">Retry</button></p>
        <div class="product-grid two"><app-product-card *ngFor="let product of page?.content" [product]="product"></app-product-card></div>
        <div class="pagination" *ngIf="page && page.totalPages > 1"><button type="button" [disabled]="page.first" (click)="go(page.number - 1)">Previous</button><span>Page {{ page.number + 1 }} of {{ page.totalPages }}</span><button type="button" [disabled]="page.last" (click)="go(page.number + 1)">Next</button></div>
      </div>
    </section>`
})
export class ProductsPage implements OnInit, OnDestroy {
  private readonly productsApi = inject(ProductService); private readonly categoriesApi = inject(CategoryService); private readonly route = inject(ActivatedRoute); private readonly destroyed$ = new Subject<void>();
  readonly search$ = new Subject<string>(); categories: Category[] = []; page: Page<Product> | null = null; loading = false; error = ''; filtersOpen = false;
  query: ProductQuery = { search: '', categoryId: undefined, minPrice: undefined, maxPrice: undefined, sort: 'newest', page: 0, size: 8 };
  ngOnInit(): void {
    const categoryId = Number(this.route.snapshot.queryParamMap.get('categoryId')); if (categoryId) this.query.categoryId = categoryId;
    this.categoriesApi.list().subscribe({ next: (items) => this.categories = items.filter((item) => item.active) });
    this.search$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroyed$)).subscribe(() => this.apply()); this.load();
  }
  ngOnDestroy(): void { this.destroyed$.next(); this.destroyed$.complete(); }
  apply(): void { this.query.page = 0; this.load(); }
  clear(): void { this.query = { search: '', sort: 'newest', page: 0, size: 8 }; this.load(); }
  go(page: number): void { this.query.page = page; this.load(); scrollTo({ top: 0, behavior: 'smooth' }); }
  load(): void { this.loading = true; this.error = ''; this.productsApi.list(this.query).pipe(finalize(() => this.loading = false)).subscribe({ next: (page) => this.page = page, error: (e: unknown) => this.error = apiMessage(e, 'Unable to load pastries.') }); }
}

@Component({
  selector: 'app-product-detail-page', standalone: true, imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="section detail" *ngIf="product; else state"><div class="detail-image"><img [src]="product.imageUrl" [alt]="product.name" (error)="useFallback($event)"></div><div class="detail-copy"><a class="eyebrow" routerLink="/products">← BACK TO COLLECTION</a><h1>{{ product.name }}</h1><p class="lede">{{ product.description }}</p>
      <ng-container *ngIf="activeVariants.length; else unavailable"><label>Choose a variant<select [(ngModel)]="selectedId"><option *ngFor="let variant of activeVariants" [ngValue]="variant.id">{{ variantLabel(variant) }} — {{ variant.price | currency:'INR' }}</option></select></label>
      <div class="detail-price">{{ selectedVariant?.price | currency:'INR':'symbol':'1.0-0' }}</div><label>Quantity<div class="stepper"><button type="button" aria-label="Decrease quantity" (click)="quantity = quantity > 1 ? quantity - 1 : 1">−</button><span aria-live="polite">{{ quantity }}</span><button type="button" aria-label="Increase quantity" (click)="quantity = quantity + 1">+</button></div></label>
      <p class="error" role="alert" aria-live="assertive" *ngIf="error">{{ error }}</p><div class="success add-success" role="status" *ngIf="message"><span>{{ message }}</span><a routerLink="/cart">View cart →</a></div><button class="primary full" type="button" [disabled]="!selectedId || adding" (click)="add()">{{ adding ? 'Adding…' : 'Add to cart' }}</button></ng-container><ng-template #unavailable><div class="unavailable"><h2>Currently unavailable</h2><p>This pastry has no active variants right now. Please check back soon or explore the rest of the collection.</p><a class="secondary" routerLink="/products">Browse other pastries</a></div></ng-template></div></section>
    <ng-template #state><section class="section"><p class="loading" *ngIf="loading">Loading pastry…</p><p class="error" *ngIf="error">{{ error }}</p></section></ng-template>`
})
export class ProductDetailPage implements OnInit {
  private readonly productsApi = inject(ProductService); private readonly cart = inject(CartService); private readonly auth = inject(AuthService); private readonly route = inject(ActivatedRoute); private readonly router = inject(Router);
  product: Product | null = null; selectedId: number | null = null; quantity = 1; loading = true; adding = false; error = ''; message = '';
  get activeVariants(): ProductVariant[] { return this.product?.variants.filter((v) => v.active) || []; }
  get selectedVariant(): ProductVariant | undefined { return this.activeVariants.find((v) => v.id === this.selectedId); }
  ngOnInit(): void { const id = Number(this.route.snapshot.paramMap.get('id')); this.productsApi.get(id).pipe(finalize(() => this.loading = false)).subscribe({ next: (product) => { this.product = product; this.selectedId = this.activeVariants[0]?.id || null; }, error: (e: unknown) => this.error = apiMessage(e, 'Pastry not found.') }); }
  variantLabel(v: ProductVariant): string { return [v.weightInGrams ? `${v.weightInGrams}g` : '', v.flavour, v.eggType === 'EGGLESS' ? 'Eggless' : v.eggType === 'EGG' ? 'With egg' : ''].filter(Boolean).join(' · '); }
  useFallback(event: Event): void { (event.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=1000&q=80'; }
  add(): void { if (!this.auth.isCustomer()) { void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } }); return; } if (!this.selectedId) return; this.adding = true; this.error = ''; this.cart.add(this.selectedId, this.quantity).pipe(finalize(() => this.adding = false)).subscribe({ next: () => { this.message = 'Added to your cart.'; this.cart.get().subscribe(); }, error: (e: unknown) => this.error = apiMessage(e, 'Unable to add this pastry.') }); }
}
