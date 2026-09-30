import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminAuthService, apiMessage } from '../core/auth';

@Component({
  selector: 'app-admin-login', standalone: true, imports: [CommonModule, ReactiveFormsModule],
  template: `
    <main class="admin-login">
      <section class="login-visual"><div class="login-brand"><span class="brand-mark large">M</span><span>MIETTE<small>PATISSERIE</small></span></div><div class="visual-copy"><span>OPERATIONS CONSOLE</span><h1>Every order,<br>beautifully managed.</h1><p>Catalog, fulfilment, and daily bakery operations in one focused workspace.</p></div><small>For authorised team members only</small></section>
      <section class="login-panel"><div class="login-form"><span class="kicker">SECURE ACCESS</span><h2>Administrator login</h2><p>Use your pastry administration account.</p><p class="notice" *ngIf="route.snapshot.queryParamMap.has('sessionExpired')">Your session expired. Sign in again.</p><p class="alert" *ngIf="error">{{ error }}</p>
        <form [formGroup]="form" (ngSubmit)="submit()"><label>Email address<input type="email" formControlName="email" autocomplete="email" placeholder="admin@example.com"></label><label>Password<input type="password" formControlName="password" autocomplete="current-password" placeholder="Enter your password"></label><button class="admin-primary full" type="submit" [disabled]="form.invalid || loading">{{ loading ? 'Signing in…' : 'Sign in to workspace' }}</button></form>
        <small class="security-note">Access is role-verified by the server. Customer accounts cannot enter this workspace.</small></div></section>
    </main>`
})
export class AdminLoginPage {
  readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder); private readonly auth = inject(AdminAuthService); private readonly router = inject(Router);
  loading = false; error = '';
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit(): void { if (this.form.invalid) return; this.loading = true; this.error = ''; const value = this.form.getRawValue(); this.auth.login(value.email, value.password).pipe(finalize(() => this.loading = false)).subscribe({ next: () => void this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard'), error: (error: unknown) => this.error = apiMessage(error, 'Unable to sign in.') }); }
}
