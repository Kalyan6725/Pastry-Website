import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { BehaviorSubject, catchError, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, User } from './models';

const TOKEN_KEY = 'pastry_admin_token';
const USER_KEY = 'pastry_admin_user';

@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly http = inject(HttpClient);
  private readonly userSubject = new BehaviorSubject<User | null>(this.readUser());
  readonly user$ = this.userSubject.asObservable();
  get user(): User | null { return this.userSubject.value; }
  get token(): string | null { return sessionStorage.getItem(TOKEN_KEY); }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(tap((response) => {
      if (response.user.role !== 'ADMIN' || !response.user.active) throw new Error('This account does not have administrator access.');
      sessionStorage.setItem(TOKEN_KEY, response.token);
      sessionStorage.setItem(USER_KEY, JSON.stringify(response.user));
      this.userSubject.next(response.user);
    }));
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.userSubject.next(null);
  }

  isAdmin(): boolean { return this.user?.role === 'ADMIN' && Boolean(this.token); }
  private readUser(): User | null {
    const value = sessionStorage.getItem(USER_KEY);
    try { return value ? JSON.parse(value) as User : null; } catch { return null; }
  }
}

export const adminInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);
  const token = auth.token;
  const outgoing = token && request.url.startsWith(environment.apiUrl)
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(outgoing).pipe(catchError((error: HttpErrorResponse) => {
    if (error.status === 401 && token) { auth.logout(); void router.navigate(['/login'], { queryParams: { sessionExpired: true } }); }
    return throwError(() => error);
  }));
};

export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AdminAuthService);
  return auth.isAdmin() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export function apiMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'The backend is unavailable. Check the connection and try again.';
    if (typeof error.error?.message === 'string') return error.error.message;
  }
  return error instanceof Error ? error.message : fallback;
}
