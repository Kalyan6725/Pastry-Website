import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { BehaviorSubject, catchError, Observable, tap, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from './models';

const TOKEN_KEY = 'pastry_customer_token';
const USER_KEY = 'pastry_customer_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly userSubject = new BehaviorSubject<User | null>(this.readUser());
  readonly user$ = this.userSubject.asObservable();

  get user(): User | null { return this.userSubject.value; }

  get token(): string | null {
    return isPlatformBrowser(this.platformId) ? sessionStorage.getItem(TOKEN_KEY) : null;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => {
        if (response.user.role !== 'CUSTOMER' || !response.user.active) {
          throw new Error('This account cannot use the customer store.');
        }
        this.storeSession(response);
      })
    );
  }

  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${environment.apiUrl}/auth/register`, request);
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    }
    this.userSubject.next(null);
  }

  isCustomer(): boolean {
    return this.user?.role === 'CUSTOMER' && Boolean(this.token);
  }

  private storeSession(response: AuthResponse): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.setItem(TOKEN_KEY, response.token);
      sessionStorage.setItem(USER_KEY, JSON.stringify(response.user));
    }
    this.userSubject.next(response.user);
  }

  private readUser(): User | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    const value = sessionStorage.getItem(USER_KEY);
    try { return value ? JSON.parse(value) as User : null; } catch { return null; }
  }
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;
  const secured = request.url.startsWith(environment.apiUrl) && token;
  return next(secured ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token) {
        auth.logout();
        void router.navigate(['/login'], { queryParams: { sessionExpired: true } });
      }
      return throwError(() => error);
    })
  );
};

export const customerGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isCustomer() ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export function apiMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'The store is unavailable. Check your connection and try again.';
    if (typeof error.error?.message === 'string') return error.error.message;
  }
  return error instanceof Error ? error.message : fallback;
}
