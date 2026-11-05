import type { AuthResponse, User } from '../types/api';

const KEY = 'qlnt.auth';
export function readAuth(): AuthResponse | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}
export function saveAuth(auth: AuthResponse) {
  sessionStorage.setItem(KEY, JSON.stringify(auth));
  window.dispatchEvent(new Event('qlnt-auth-changed'));
}
export function clearAuth() {
  sessionStorage.removeItem(KEY);
  window.dispatchEvent(new Event('qlnt-auth-changed'));
}
export function currentUser(): User | null {
  return readAuth()?.user ?? null;
}
