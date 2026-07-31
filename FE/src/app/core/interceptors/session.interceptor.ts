import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  const apiReq = req.clone({
    withCredentials: true
  });

  return next(apiReq).pipe(
    catchError(error => {
      if (error.status === 401) {
        router.navigate(['/login']);
      } else if (error.status === 500) {
        const serverMsg = error.error?.message ?? error.message;
        console.error('Error del servidor:', serverMsg);
      }
      return throwError(() => error);
    })
  );
};
