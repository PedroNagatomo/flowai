import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: () => boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isAuthenticated: () => !!get().token,
      setAuth: (token, user) => {
        localStorage.setItem('flowai_token', token);
        set({ token, user });
      },
      logout: () => {
        localStorage.removeItem('flowai_token');
        localStorage.removeItem('flowai_user');
        set({ token: null, user: null });
      },
    }),
    {
      name: 'flowai_auth',
      partialize: (state) => ({ token: state.token, user: state.user }),
    }
  )
);