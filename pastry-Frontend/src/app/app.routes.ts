import { Routes } from '@angular/router';
import { CustomerLayout } from './layout/customer-layout';
import { LoginPage, RegisterPage } from './features/auth-pages';
import { HomePage, ProductDetailPage, ProductsPage } from './features/catalog-pages';
import { customerGuard } from './core/auth';
import { AddressesPage, CartPage, CheckoutPage, OrderDetailPage, OrdersPage } from './features/shopping-pages';

export const routes: Routes = [
	{ path: 'login', component: LoginPage },
	{ path: 'register', component: RegisterPage },
	{
		path: '', component: CustomerLayout, children: [
			{ path: '', pathMatch: 'full', redirectTo: 'home' },
			{ path: 'home', component: HomePage },
			{ path: 'products', component: ProductsPage },
			{ path: 'products/:id', component: ProductDetailPage },
			{ path: 'cart', component: CartPage, canActivate: [customerGuard] },
			{ path: 'checkout', component: CheckoutPage, canActivate: [customerGuard] },
			{ path: 'addresses', component: AddressesPage, canActivate: [customerGuard] },
			{ path: 'orders', component: OrdersPage, canActivate: [customerGuard] },
			{ path: 'orders/:id', component: OrderDetailPage, canActivate: [customerGuard] }
		]
	},
	{ path: '**', redirectTo: 'home' }
];
