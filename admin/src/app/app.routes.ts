import { Routes } from '@angular/router';
import { adminGuard } from './core/auth';
import { CategoriesPage } from './features/categories-page';
import { DashboardPage } from './features/dashboard-page';
import { AdminLoginPage } from './features/login-page';
import { AdminOrderDetailPage, AdminOrdersPage } from './features/order-pages';
import { AdminProductsPage, ProductFormPage } from './features/product-pages';
import { AdminLayout } from './layout/admin-layout';

export const routes: Routes = [
	{ path: 'login', component: AdminLoginPage },
	{
		path: '', component: AdminLayout, canActivate: [adminGuard], children: [
			{ path: '', pathMatch: 'full', redirectTo: 'dashboard' },
			{ path: 'dashboard', component: DashboardPage },
			{ path: 'products', component: AdminProductsPage },
			{ path: 'products/new', component: ProductFormPage },
			{ path: 'products/:id/edit', component: ProductFormPage },
			{ path: 'categories', component: CategoriesPage },
			{ path: 'orders', component: AdminOrdersPage },
			{ path: 'orders/:id', component: AdminOrderDetailPage }
		]
	},
	{ path: '**', redirectTo: 'dashboard' }
];
