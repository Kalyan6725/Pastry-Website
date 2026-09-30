import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { apiMessage } from '../core/auth';
import { AdminCategoryService } from '../core/api';
import { Category } from '../core/models';

@Component({
  selector: 'app-categories-page', standalone: true, imports: [CommonModule, ReactiveFormsModule],
  template: `
    <header class="admin-page-head"><div><span class="kicker">CATALOG</span><h1>Categories</h1><p>Organise the customer collection into clear, browsable groups.</p></div><button class="admin-primary" type="button" (click)="beginCreate()">Add category</button></header>
    <p class="alert" role="alert" aria-live="assertive" *ngIf="error">{{ error }}</p><p class="contract-note">Only active categories can be retrieved with the current backend API. A deactivated category disappears from this list and requires a backend admin-read endpoint to restore through the UI.</p>
    <section class="admin-panel"><div class="panel-head"><div><h2>Visible categories</h2><span>{{ categories.length }} active categories</span></div></div><p class="loading-state" *ngIf="loading">Loading categories…</p><div class="empty-state" *ngIf="!loading && !categories.length"><strong>No active categories</strong><span>Create a category to begin building the catalog.</span></div>
      <div class="category-admin-grid"><article *ngFor="let item of categories"><div class="category-number">{{ item.name.charAt(0) }}</div><div><span class="status-pill" data-status="ACTIVE">Active</span><h3>{{ item.name }}</h3><p>{{ item.description || 'No description provided.' }}</p></div><div class="card-actions"><button type="button" (click)="beginEdit(item)">Edit</button><button class="danger" type="button" (click)="requestDeactivate(item)">Deactivate</button></div></article></div>
    </section>
    <div class="modal-backdrop" *ngIf="editing" (click)="close()"><section class="admin-modal" role="dialog" aria-modal="true" aria-labelledby="category-title" (click)="$event.stopPropagation()"><div class="modal-head"><div><span class="kicker">{{ editId ? 'UPDATE' : 'CREATE' }}</span><h2 id="category-title">{{ editId ? 'Edit category' : 'New category' }}</h2></div><button type="button" aria-label="Close" (click)="close()">×</button></div><form [formGroup]="form" (ngSubmit)="save()"><label>Category name<input autofocus formControlName="name" maxlength="100" placeholder="e.g. Celebration cakes"></label><label>Description<textarea formControlName="description" maxlength="1000" rows="5" placeholder="A short customer-facing description"></textarea></label><div class="modal-actions"><button class="admin-secondary" type="button" (click)="close()">Cancel</button><button class="admin-primary" type="submit" [disabled]="form.invalid || saving">{{ saving ? 'Saving…' : 'Save category' }}</button></div></form></section></div>
    <div class="modal-backdrop" *ngIf="pendingDeactivate" (click)="pendingDeactivate = null"><section class="admin-modal confirm-modal" role="alertdialog" aria-modal="true" aria-labelledby="deactivate-category-title" (click)="$event.stopPropagation()"><span class="kicker">DEACTIVATE CATEGORY</span><h2 id="deactivate-category-title">Remove {{ pendingDeactivate.name }} from the store?</h2><p>Products in this category will no longer appear to customers. The current backend does not provide a way to restore it from this admin UI.</p><div class="modal-actions"><button class="admin-secondary" type="button" (click)="pendingDeactivate = null">Keep active</button><button class="admin-primary destructive" type="button" [disabled]="saving" (click)="confirmDeactivate()">{{ saving ? 'Deactivating…' : 'Deactivate category' }}</button></div></section></div>`
})
export class CategoriesPage implements OnInit {
  private readonly api = inject(AdminCategoryService); private readonly fb = inject(FormBuilder);
  categories: Category[] = []; loading = true; saving = false; editing = false; editId: number | null = null; pendingDeactivate: Category | null = null; error = '';
  readonly form = this.fb.nonNullable.group({ name: ['', [Validators.required, Validators.maxLength(100)]], description: ['', Validators.maxLength(1000)] });
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.api.listVisible().pipe(finalize(() => this.loading = false)).subscribe({ next: (items) => this.categories = items, error: (error: unknown) => this.error = apiMessage(error, 'Unable to load categories.') }); }
  beginCreate(): void { this.editId = null; this.form.reset({ name: '', description: '' }); this.editing = true; }
  beginEdit(item: Category): void { this.editId = item.id; this.form.reset({ name: item.name, description: item.description || '' }); this.editing = true; }
  close(): void { if (!this.saving) this.editing = false; }
  @HostListener('document:keydown.escape') onEscape(): void { if (!this.saving) { this.editing = false; this.pendingDeactivate = null; } }
  save(): void { if (this.form.invalid) return; this.saving = true; this.error = ''; const request = this.editId ? this.api.update(this.editId, this.form.getRawValue()) : this.api.create(this.form.getRawValue()); request.pipe(finalize(() => this.saving = false)).subscribe({ next: () => { this.close(); this.load(); }, error: (error: unknown) => this.error = apiMessage(error, 'Unable to save category.') }); }
  requestDeactivate(item: Category): void { this.pendingDeactivate = item; }
  confirmDeactivate(): void { if (!this.pendingDeactivate) return; this.saving = true; this.api.setActive(this.pendingDeactivate.id, false).pipe(finalize(() => this.saving = false)).subscribe({ next: () => { this.pendingDeactivate = null; this.load(); }, error: (error: unknown) => this.error = apiMessage(error, 'Unable to deactivate category.') }); }
}
