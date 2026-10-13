import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { cloudinaryImageUrl } from '../core/image-url';
import { Product } from '../core/models';

@Component({
  selector: 'app-product-card', standalone: true, imports: [CommonModule, RouterLink],
  template: `
    <article class="product-card">
      <a [routerLink]="['/products', product.id]" class="product-image">
        <img
          [src]="imageUrl(product.imageUrl, 640)"
          [srcset]="imageSrcSet(product.imageUrl)"
          sizes="(max-width: 767px) 100vw, (max-width: 1199px) 50vw, 25vw"
          [alt]="product.name"
          width="640"
          height="480"
          loading="lazy"
          decoding="async">
      </a>
      <div class="product-copy">
        <span class="eyebrow">{{ product.categoryName }}</span>
        <h3><a [routerLink]="['/products', product.id]">{{ product.name }}</a></h3>
        <p>{{ product.description || 'Freshly prepared in our kitchen.' }}</p>
        <div class="product-foot"><strong *ngIf="minimumPrice !== null">From {{ minimumPrice | currency:'INR':'symbol':'1.0-0' }}</strong><a class="text-link" [routerLink]="['/products', product.id]">View pastry →</a></div>
      </div>
    </article>`
})
export class ProductCard {
  @Input() product!: Product;
  imageUrl(imageUrl: string, width = 640): string { return cloudinaryImageUrl(imageUrl, width); }
  imageSrcSet(imageUrl: string): string {
    return [320, 480, 640, 960].map((width) => `${this.imageUrl(imageUrl, width)} ${width}w`).join(', ');
  }
  get minimumPrice(): number | null {
    const prices = this.product.variants.filter((variant) => variant.active).map((variant) => variant.price);
    return prices.length ? Math.min(...prices) : null;
  }
}
