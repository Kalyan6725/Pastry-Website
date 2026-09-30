import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { adminInterceptor } from './core/auth';

export const appConfig = {
  providers: [
    provideHttpClient(withInterceptors([adminInterceptor])),
    provideRouter(routes)
  ]
};
