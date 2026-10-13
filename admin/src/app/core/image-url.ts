import { environment } from '../../environments/environment';

export function resolveImageUrl(imageUrl: string | null | undefined): string {
  if (!imageUrl) return '';
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl;
  const backendOrigin = environment.apiUrl.replace(/\/api\/?$/, '');
  return `${backendOrigin}/${imageUrl.replace(/^\/+/, '')}`;
}

export function cloudinaryImageUrl(imageUrl: string | null | undefined, width: number, height = Math.round(width * 0.75)): string {
  const resolved = resolveImageUrl(imageUrl);
  if (!/\/image\/upload\//i.test(resolved)) return resolved;
  return resolved.replace('/image/upload/', `/image/upload/f_auto,q_auto,c_fill,g_auto,w_${width},h_${height}/`);
}