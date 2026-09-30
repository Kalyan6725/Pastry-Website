import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiMessage, AuthService } from '../core/auth';
import { CartService } from '../core/api';

@Component({
  selector: 'app-login-page', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <div class="auth-image" role="img" aria-label="Fresh fruit pastries"></div>
      <div class="auth-panel">
        <a class="eyebrow" routerLink="/home">MIETTE PATISSERIE</a>
        <h1>Welcome back</h1><p class="lede">Your next favourite pastry is waiting.</p>
        <p class="success" role="status" *ngIf="route.snapshot.queryParamMap.has('registered')">Account created. Sign in to start shopping.</p>
        <p class="notice" *ngIf="route.snapshot.queryParamMap.has('sessionExpired')">Your session expired. Please sign in again.</p>
        <p class="error" role="alert" aria-live="assertive" *ngIf="error">{{ error }}</p>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <label>Email<input type="email" formControlName="email" autocomplete="email" placeholder="you@example.com" [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid"></label>
          <small class="field-error" *ngIf="form.controls.email.touched && form.controls.email.invalid">Enter a valid email address.</small>
          <label>Password<input type="password" formControlName="password" autocomplete="current-password" [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid"></label>
          <small class="field-error" *ngIf="form.controls.password.touched && form.controls.password.invalid">Password is required.</small>
          <button class="primary full" type="submit" [disabled]="form.invalid || loading">{{ loading ? 'Signing in…' : 'Sign in' }}</button>
        </form>
        <p class="auth-switch">New to Miette? <a routerLink="/register">Create an account</a></p>
      </div>
    </section>`
})
export class LoginPage {
  readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  loading = false; error = '';
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  submit(): void {
    if (this.form.invalid) return;
    this.loading = true; this.error = '';
    this.auth.login(this.form.getRawValue()).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => {
        this.cart.get().subscribe({ error: () => this.cart.clearCount() });
        void this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('returnUrl') || '/home');
      },
      error: (error: unknown) => this.error = apiMessage(error, 'Unable to sign in.')
    });
  }
}

@Component({
  selector: 'app-register-page', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page reverse">
      <div class="auth-image register" role="img" aria-label="Pastry chef decorating a cake"></div>
      <div class="auth-panel">
        <a class="eyebrow" routerLink="/home">MIETTE PATISSERIE</a>
        <h1>Join the table</h1><p class="lede">Create your customer account.</p>
        <p class="error" role="alert" aria-live="assertive" *ngIf="error">{{ error }}</p>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <label>Name<input formControlName="name" autocomplete="name" maxlength="100" [attr.aria-invalid]="form.controls.name.touched && form.controls.name.invalid"></label>
          <small class="field-error" *ngIf="form.controls.name.touched && form.controls.name.invalid">Name is required.</small>
          <label>Email<input type="email" formControlName="email" autocomplete="email" [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid"></label>
          <small class="field-error" *ngIf="form.controls.email.touched && form.controls.email.invalid">Enter a valid email address.</small>
          <label>Password<input type="password" formControlName="password" autocomplete="new-password" [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid"></label>
          <small class="field-error" *ngIf="form.controls.password.touched && form.controls.password.invalid">Use at least 6 characters.</small>
          <label>Confirm password<input type="password" formControlName="confirmPassword" autocomplete="new-password"></label>
          <small class="field-error" *ngIf="form.controls.confirmPassword.touched && form.value.password !== form.value.confirmPassword">Passwords must match.</small>
          <button class="primary full" type="submit" [disabled]="form.invalid || form.value.password !== form.value.confirmPassword || loading">{{ loading ? 'Creating account…' : 'Create account' }}</button>
        </form>
        <p class="auth-switch">Already registered? <a routerLink="/login">Sign in</a></p>
      </div>
    </section>`
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  loading = false; error = '';
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]], email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]], confirmPassword: ['', Validators.required]
  });
  submit(): void {
    if (this.form.invalid || this.form.value.password !== this.form.value.confirmPassword) return;
    this.loading = true; this.error = '';
    const { name, email, password } = this.form.getRawValue();
    this.auth.register({ name, email, password }).pipe(finalize(() => this.loading = false)).subscribe({
      next: () => void this.router.navigate(['/login'], { queryParams: { registered: true } }),
      error: (error: unknown) => this.error = apiMessage(error, 'Unable to create your account.')
    });
  }
}
